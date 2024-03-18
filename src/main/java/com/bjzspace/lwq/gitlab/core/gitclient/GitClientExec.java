package com.bjzspace.lwq.gitlab.core.gitclient;

import com.bjzspace.lwq.gitlab.config.GitLabInfoConfig;
import com.bjzspace.lwq.gitlab.constant.Constants;
import com.bjzspace.lwq.gitlab.core.GitClient;
import org.gitlab4j.api.models.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class GitClientExec implements GitClient {
    public static final Logger LOGGER = LoggerFactory.getLogger(GitClientExec.class);

    private GitLabInfoConfig gitLabInfoConfig;

    public GitClientExec(GitLabInfoConfig gitLabInfoConfig) {
        this.gitLabInfoConfig = gitLabInfoConfig;
    }

    @Override
    public void clone(String branchName, Project project, File execDir) {
        String httpUrlToRepo = project.getHttpUrlToRepo();
        if(httpUrlToRepo.startsWith(Constants.PROTOCOL_HTTPS)){
            httpUrlToRepo = Constants.PROTOCOL_HTTPS + Constants.OAUTH2 + gitLabInfoConfig.getAccessToken() + "@" +httpUrlToRepo.substring(Constants.PROTOCOL_HTTPS.length());
        }else if(httpUrlToRepo.startsWith(Constants.PROTOCOL_HTTP)){
            httpUrlToRepo = Constants.PROTOCOL_HTTP + Constants.OAUTH2 +  gitLabInfoConfig.getAccessToken() + "@" +httpUrlToRepo.substring(Constants.PROTOCOL_HTTP.length());
        }
        String command = String.format(Constants.GIT_CLONE_BRANCH, branchName, httpUrlToRepo);
        LOGGER.info("clone command:{}", command);
        try {
            Process exec = Runtime.getRuntime().exec(command, null, execDir);
            exec.waitFor();
            String successResult = inputStreamToString(exec.getInputStream());
            LOGGER.info("clone successResult:{}", successResult);
            String errorResult = inputStreamToString(exec.getErrorStream());
            LOGGER.info("clone errorResult:{}", errorResult);
            System.out.println("================================");
        } catch (IOException | InterruptedException e) {
            LOGGER.error("clone command error!", e);
        }
    }
    private String inputStreamToString(final InputStream input) {
        StringBuilder result = new StringBuilder();
        Reader reader = new InputStreamReader(input);
        BufferedReader bf = new BufferedReader(reader);
        String line;
        try {
            while ((line = bf.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
