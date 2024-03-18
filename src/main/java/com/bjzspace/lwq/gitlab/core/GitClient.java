package com.bjzspace.lwq.gitlab.core;

import org.gitlab4j.api.models.Project;

import java.io.File;

public interface GitClient {

    public void clone(String branchName, Project project, File execDir);

}
