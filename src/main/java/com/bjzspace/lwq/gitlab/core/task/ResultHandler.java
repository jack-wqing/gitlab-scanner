package com.bjzspace.lwq.gitlab.core.task;

import cn.hutool.json.JSONUtil;
import com.bjzspace.lwq.gitlab.config.GitLabInfoConfig;
import com.bjzspace.lwq.gitlab.core.excel.ExcelExecOperator;
import com.bjzspace.lwq.gitlab.core.gitlab.GitLabExecutor;
import com.bjzspace.lwq.gitlab.entity.ProjectScannerResult;
import com.bjzspace.lwq.gitlab.entity.ScanResult;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.models.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ResultHandler extends BaseDaemonThread{

    public static final Logger LOGGER = LoggerFactory.getLogger(ResultHandler.class);

    @Resource
    private GitLabInfoConfig gitLabInfoConfig;
    @Resource
    private ResultQueue resultQueue;
    public static volatile  ConcurrentHashMap<String,ConcurrentHashMap<Long, Project>> batchProjectMap = new ConcurrentHashMap<>();
    public static volatile  ConcurrentHashMap<String,ConcurrentHashMap<Long, AtomicInteger>> projectBranchCount = new ConcurrentHashMap<>();
    public static volatile  ConcurrentHashMap<String,ConcurrentHashMap<Long, ConcurrentHashMap<String, ScanResult>>> projectBranchResult= new ConcurrentHashMap<>();

    protected ResultHandler() {
        super("ResultHandler-thread");
    }

    @PostConstruct
    public void start(){
        super.start();
    }

    @Override
    public void run() {
        while (true){
            try {
                String batchId = resultQueue.poolEvent();
                ConcurrentHashMap<Long, AtomicInteger> projectBranchCountMap =
                    ResultHandler.projectBranchCount(batchId);
                if(projectBranchCountMap == null || CollectionUtils.isEmpty(projectBranchCountMap.keySet())){
                    continue;
                }

                projectBranchCountMap.forEach((k, v) -> {
                    Long projectId = k;
                    List<BranchResultInfo> brInfos = Lists.newArrayList();
                    AtomicBoolean keyWords = new AtomicBoolean(false);
                    if(v.get() > 0){
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        resultQueue.addEvent(batchId);
                        return ;
                    }
                    ConcurrentHashMap<String, ScanResult> projectResultInfo = ResultHandler.projectBranchResult(batchId).get(projectId);
                    projectResultInfo.forEach((bN, br)->{
                        if(br.isFind()){
                            keyWords.set(true);
                        }
                        brInfos.add(new BranchResultInfo(br.getRootPath(), br.getFileName(), br.getLineNumber(), br.isFind() ? "是" : "否"));
                    });
                    Project project = ResultHandler.project(batchId).get(projectId);
                    ProjectScannerResult scannerResult = new ProjectScannerResult();
                    scannerResult.setId(project.getId());
                    scannerResult.setName(project.getName());
                    scannerResult.setOwner(GitLabExecutor.userMap.get(project.getCreatorId()));
                    scannerResult.setBranchInfo(JSONUtil.toJsonStr(brInfos));
                    scannerResult.setContainKeyWords(keyWords.get() ? "是" :  "否");
                    scannerResult.setHttpUrl(project.getHttpUrlToRepo());
                    ExcelExecOperator.writeProjectScannerExcel(gitLabInfoConfig.getWorkSpace() + batchId + "-scanner.xlsx",
                        "scanner-result", Arrays.asList(scannerResult));
                    // 删除完成的项目
                    ResultHandler.project(batchId).remove(projectId);
                    ResultHandler.projectBranchCount(batchId).remove(projectId);
                    ResultHandler.projectBranchResult(batchId).remove(projectId);
                    if(gitLabInfoConfig.isDeleteOkProject()) {
                        try {
                            String name = project.getName();
                            name = name.replaceAll(" ", "=");
                            String projectDir = gitLabInfoConfig.getWorkSpace() + project.getId() + "_" + name;
                            URI uri = new URI(projectDir);
                            File dir = new File(uri.getPath());
                            if (dir.exists()) {
                                try {
                                    FileUtils.deleteDirectory(dir);
                                    LOGGER.info("delete directory : {} -> success", dir.getAbsolutePath());
                                } catch (IOException ioe) {
                                    LOGGER.info("delete directory : {} -> error", dir.getAbsolutePath());
                                }
                            }
                        } catch (URISyntaxException e) {
                            LOGGER.info("delete directory URISyntaxException");
                        }
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.error("result queue InterruptedException", e);
            }
        }
    }

    public static synchronized ConcurrentHashMap<Long, Project> project(String batchId){
        ConcurrentHashMap<Long, Project> projectMap = batchProjectMap.get(batchId);

        if(projectMap == null){
            synchronized (ResultHandler.class){
                projectMap = batchProjectMap.get(batchId);
                if(projectMap == null){
                    projectMap = new ConcurrentHashMap<>();
                    batchProjectMap.put(batchId, projectMap);
                }
            }
        }
        return projectMap;
    }

    public static synchronized ConcurrentHashMap<Long, AtomicInteger> projectBranchCount(String batchId){
        ConcurrentHashMap<Long, AtomicInteger> projectBranchCountMap = projectBranchCount.get(batchId);
        if(projectBranchCountMap == null){
            synchronized (ResultHandler.class){
                projectBranchCountMap = projectBranchCount.get(batchId);
                if(projectBranchCountMap == null){
                    projectBranchCountMap = new ConcurrentHashMap<>();
                    projectBranchCount.put(batchId, projectBranchCountMap);
                }
            }
        }
        return projectBranchCountMap;
    }

    public static synchronized ConcurrentHashMap<Long, ConcurrentHashMap<String, ScanResult>> projectBranchResult(String batchId){
        ConcurrentHashMap<Long, ConcurrentHashMap<String, ScanResult>> projectBranchResultMap = projectBranchResult.get(batchId);
        if(projectBranchResultMap == null){
            synchronized (ResultHandler.class){
                projectBranchResultMap = projectBranchResult.get(batchId);
                if(projectBranchResultMap == null){
                    projectBranchResultMap = new ConcurrentHashMap<>();
                    projectBranchResult.put(batchId, projectBranchResultMap);
                }
            }
        }
        return projectBranchResultMap;
    }

    @Getter
    @Setter
    class BranchResultInfo{
        private String branName;
        private String find;
        private String fileName;
        private Long lineNumber;

        public BranchResultInfo(String branName,String fileName, Long lineNumber,  String find) {
            this.branName = branName;
            this.find = find;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
        }
    }

}
