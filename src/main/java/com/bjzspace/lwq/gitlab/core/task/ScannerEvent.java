package com.bjzspace.lwq.gitlab.core.task;

import com.bjzspace.lwq.gitlab.config.FileRuleConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gitlab4j.api.models.Project;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ScannerEvent {

    private Project project;

    private String projectPath;

    private String batchId;

    private FileRuleConfig fileRuleConfig;

}
