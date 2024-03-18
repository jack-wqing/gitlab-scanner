package com.bjzspace.lwq.gitlab.core.gitlab;

import cn.hutool.json.JSONUtil;
import com.bjzspace.lwq.gitlab.config.FileRuleConfig;
import com.bjzspace.lwq.gitlab.config.GitLabInfoConfig;
import com.bjzspace.lwq.gitlab.constant.Constants;
import com.bjzspace.lwq.gitlab.core.GitClient;
import com.bjzspace.lwq.gitlab.core.excel.ExcelExecOperator;
import com.bjzspace.lwq.gitlab.core.gitclient.GitClientExec;
import com.bjzspace.lwq.gitlab.core.task.ScannerEvent;
import com.bjzspace.lwq.gitlab.core.task.ScannerQueue;
import com.bjzspace.lwq.gitlab.entity.ProjectSimpleInfo;
import com.google.common.collect.Lists;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class GitLabExecutor implements InitializingBean {

    public static final Logger LOGGER = LoggerFactory.getLogger(GitLabExecutor.class);

    @Resource
    private GitLabInfoConfig gitLabInfoConfig;

    private GitClient gitClient;
    private GitLabApi gitLabApi;

    @Resource
    private ScannerQueue scannerQueue;

    public static final Map<Long, String> userMap = new HashMap<>();


    private static final ThreadPoolExecutor executorService = (ThreadPoolExecutor)Executors.newFixedThreadPool(1);
    private static final ThreadPoolExecutor projectCloneExecutorService = (ThreadPoolExecutor)Executors.newFixedThreadPool(3);
    private static final ThreadPoolExecutor cloneExecutorService = (ThreadPoolExecutor)Executors.newFixedThreadPool(9);

    public String pullProject() throws GitLabApiException {
        String workSpace = gitLabInfoConfig.getWorkSpace();
        String projectList = gitLabInfoConfig.getProjectList();
        if(!StringUtils.hasText(workSpace) || !StringUtils.hasText(projectList)){
            return "gitLabInfoConfig config error, workSpace is blank or projectList is blank";
        }
        if(!projectList.endsWith(Constants.EXCEL_XLSX)){
            return "gitLabInfoConfig.ProjectList must excel[xlsx]";
        }

        List<Project> projects = gitLabApi.getProjectApi().getProjects();
        List<User> activeUsers = gitLabApi.getUserApi().getActiveUsers();
        Map<Long, String> userMap =  activeUsers.stream().collect(Collectors.toMap(User::getId, User::getName));
        LOGGER.info("pull project size:" + projects.size());
        List<ProjectSimpleInfo> list = Lists.newArrayList();
        for (Project project : projects) {
            ProjectSimpleInfo info = new ProjectSimpleInfo();
            info.setId(project.getId());
            info.setName(project.getName());
            Long creatorId = project.getCreatorId();
            info.setOwner(creatorId == null ? "" : userMap.get(creatorId));
            info.setVisibility(project.getVisibility().toValue());
            info.setHttpUrl(project.getHttpUrlToRepo());
            list.add(info);
        }
        String filePathName = workSpace + projectList;
        ExcelExecOperator.writeProjectExcel(filePathName, "project-list", list);
        return "please look dir:" + filePathName;
    }


    public String submit(FileRuleConfig fileRuleConfig){
        if(fileRuleConfig == null){
            LOGGER.info("fileRuleConfig == null and return");
            return "参数不对";
        }
        if(!StringUtils.hasText(fileRuleConfig.getKeyWords())){
            LOGGER.info("fileRuleConfig.keyWords is blank and return");
            return "参数不对";
        }
        int size = executorService.getQueue().size();
        if(size > 6){
            return "提交的扫描任务过多，稍后在提交";
        }
        executorService.execute(()-> {
            try {
                doExecute(fileRuleConfig);
            } catch (GitLabApiException e) {
                LOGGER.error("GitLabExecutor.doExecute error", e);
            }
        });
        return "提交成功";
    }

    public void doExecute(FileRuleConfig fileRuleConfig) throws GitLabApiException {
        String uuid = UUID.randomUUID().toString();
        String workSpace = gitLabInfoConfig.getWorkSpace();
        String projectList = gitLabInfoConfig.getProjectList();
        if(StringUtils.hasText(projectList)){
            if(projectList.endsWith(Constants.EXCEL_XLSX)){
                File file = new File(workSpace + projectList);
                if(file.exists()){
                    List<ProjectSimpleInfo> list = ExcelExecOperator.readProject(file);
                    handleFindProjects(list, uuid, fileRuleConfig);
                }else{
                    LOGGER.info("从配置的项目文件找到的文件不存在");
                }
            }else{
                LOGGER.info("从配置的项目文件找到的文件不是excel xlsx");
            }
        }else{
            handleProjects(uuid, fileRuleConfig);
        }
    }

    private void handleFindProjects(List<ProjectSimpleInfo> list, String uuid, FileRuleConfig fileRuleConfig) throws GitLabApiException {
        for (ProjectSimpleInfo info : list) {
            Project project = gitLabApi.getProjectApi().getProject(info.getId());
            projectCloneExecutorService.execute(() -> {
                try {
                    handSingleProject(project, uuid, fileRuleConfig);
                } catch (GitLabApiException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void handleProjects(String uuid, FileRuleConfig fileRuleConfig) throws GitLabApiException {
        List<Project> projects = gitLabApi.getProjectApi().getProjects();
        for (Project project : projects) {
            projectCloneExecutorService.execute(() -> {
                try {
                    handSingleProject(project, uuid, fileRuleConfig);
                } catch (GitLabApiException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void handSingleProject(Project project, String uuid, FileRuleConfig fileRuleConfig)
        throws GitLabApiException {
        LOGGER.info("clone project: id:{}, name:{}, batchId:{}, FileRuleConfig:{}", project.getId(), project.getName(), uuid,
            JSONUtil.toJsonStr(fileRuleConfig));
        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(project.getId());
        sort(branches);
        // 提取最新的分支
        Integer scanBranchCount = gitLabInfoConfig.getScanBranchCount();
        int total = branches.size();
        if(scanBranchCount != null && scanBranchCount.intValue() > 0){
            if(branches.size() > scanBranchCount.intValue()){
                branches = branches.subList(0, 3);
            }
        }
        LOGGER.info("clone total branch size:{}, select branch size:{}", total, branches.size());
        String name = project.getName();
        name = name.replaceAll(" ", "=");
        String projectDirString = gitLabInfoConfig.getWorkSpace() + project.getId() + "_" + name;
        File projectDir = new File(projectDirString);
        if(!projectDir.exists()){
            projectDir.mkdirs();
        }
        CountDownLatch cdl = new CountDownLatch(branches.size());
        for (Branch branch : branches) {
            String branchName = branch.getName();
            branchName = branchName.replaceAll(" ", "=");
            branchName = branchName.replaceAll("/", "@");
            String dirName = projectDirString + "/" + branchName;
            File dir = new File(dirName);
            if(!dir.exists()){
                dir.mkdirs();
            }else{
                // 如果已经克隆就不再克隆
                cdl.countDown();
                continue;
            }
            cloneExecutorService.execute(() -> {
                try{
                    LOGGER.info("clone branch projectName:{}, branchName:{}, dir:{}", project.getName(), branch.getName(), dirName);
                    gitClient.clone(branch.getName(), project, dir);
                }catch (Exception e){
                    LOGGER.error("cloneExecutorService clone error!", e);
                }finally {
                    cdl.countDown();
                }
            });
        }
        try {
            cdl.await(12, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            LOGGER.error("cloneExecutorService CountDownLatch cdl error!", e);
        }
        scannerQueue.addEvent(new ScannerEvent(project, projectDirString, uuid, fileRuleConfig));
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        String workSpace = this.gitLabInfoConfig.getWorkSpace();
        if(!workSpace.endsWith(Constants.BACKSLASH)){
            workSpace = workSpace + Constants.BACKSLASH;
        }
        gitLabInfoConfig.setWorkSpace(workSpace);
        gitLabApi = new GitLabApi(gitLabInfoConfig.getGitLabDomain(), gitLabInfoConfig.getAccessToken());
        gitClient = new GitClientExec(gitLabInfoConfig);
        List<User> activeUsers = gitLabApi.getUserApi().getActiveUsers();
        for (User activeUser : activeUsers) {
            userMap.put(activeUser.getId(), activeUser.getName());
        }
        validate();
    }

    private void sort(List<Branch> branches){
        Collections.sort(branches, (b1, b2) -> {
            Date b1commit = b1.getCommit().getCommittedDate();
            Date b2commit = b2.getCommit().getCommittedDate();
            return  b2commit.getTime() > b1commit.getTime() ? 1 : -1;
        });

    }

    private void validate(){
        Assert.hasText(gitLabInfoConfig.getGitLabDomain(), "gitLabDomain must has text");
        Assert.hasText(gitLabInfoConfig.getAccessToken(), "accessToken must has text");
        Assert.hasText(gitLabInfoConfig.getWorkSpace(), "workSpace must has text");
    }
}
