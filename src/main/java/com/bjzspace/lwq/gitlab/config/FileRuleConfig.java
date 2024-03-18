package com.bjzspace.lwq.gitlab.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileRuleConfig {

    private String skipFileSuffix = "pdf,xml,word,xlsx,xlx,wps,jar,doc,docx,zip,tar,gz,txt,png,jpg,jpeg,gif";

    private String mustFileSuffix = "java, js, py, yaml, yml, properties, h, cpp";

    private String keyWords = "";

    private String excludeFileName="";

}
