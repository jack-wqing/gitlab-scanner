package com.bjzspace.lwq.gitlab.core;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.bjzspace.lwq.gitlab.config.GitLabInfoConfig;
import com.bjzspace.lwq.gitlab.core.excel.ExcelExecOperator;
import com.bjzspace.lwq.gitlab.core.gitclient.GitClientExec;
import com.bjzspace.lwq.gitlab.entity.ProjectSimpleInfo;
import com.google.common.collect.Lists;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GitLabApiTest {

    private GitLabInfoConfig gitLabInfoConfig = new GitLabInfoConfig();

    private GitLabApi gitLabApi;

    private GitClient gitClient = new GitClientExec(gitLabInfoConfig);


    @Before
    public void testBefore(){
        gitLabApi = new GitLabApi(gitLabInfoConfig.getGitLabDomain(), gitLabInfoConfig.getAccessToken());
    }


    @Test
    public void getNamespaceApi() throws GitLabApiException {
        List<Namespace> namespaces = gitLabApi.getNamespaceApi().getNamespaces();
        namespaces.forEach(ns -> {
            System.out.println(JSONUtil.toJsonStr(ns));
        });



    }

    @Test
    public void getProjectApi() throws GitLabApiException, IOException {
        List<Project> projects = gitLabApi.getProjectApi().getProjects();

        List<User> activeUsers = gitLabApi.getUserApi().getActiveUsers();

        Map<Long, String> userMap =  activeUsers.stream().collect(Collectors.toMap(User::getId, User::getName));

        System.out.println("project size:" + projects.size());
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
        String uuid = UUID.randomUUID().toString();
        String filePathName = "/Users/qing/gitlab/" + uuid + ".xlsx";
        ExcelExecOperator.writeProjectExcel(filePathName, "project-list", list);

    }


    @Test
    public void cloneProject() throws GitLabApiException {

        List<Project> projects = gitLabApi.getProjectApi().getProjects(1, 1);
        //Project project = gitLabApi.getProjectApi().getProject("4133");
        Project project = projects.get(0);
        System.out.println(JSONUtil.toJsonStr(project));
        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(project.getId());

        String projectDirString = "/Users/qing/gitlab/" + project.getName();
        File projectDir = new File(projectDirString);
        if(!projectDir.exists()){
            projectDir.mkdirs();
        }



        for (Branch branch : branches) {
            String dirName = projectDirString + "/" + branch.getName();
            System.out.println(dirName);
            File dir = new File(dirName);
            if(!dir.exists()){
                dir.mkdirs();
            }
            gitClient.clone(branch.getName(), project, dir);
        }

    }




}
