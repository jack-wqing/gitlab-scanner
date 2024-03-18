package com.bjzspace.lwq.gitlab.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * gitlab 的访问信息配置
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gitlab")
public class GitLabInfoConfig {

    private String gitLabDomain;

    private String accessToken;

    private String workSpace;

    // 表示本任务将要处理的任务列表 excel路径，可以该系统代码下载该excel,为空表示全部处理
    private String projectList;

    //扫描的分支数
    private Integer scanBranchCount;
    // 是否删除成功完成的项目文件
    private boolean deleteOkProject;

}
