package com.bjzspace.lwq.gitlab.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GitLabInfoConfig.class)
public class AppConfig {

}
