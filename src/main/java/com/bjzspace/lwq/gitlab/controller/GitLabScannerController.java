package com.bjzspace.lwq.gitlab.controller;

import com.bjzspace.lwq.gitlab.config.FileRuleConfig;
import com.bjzspace.lwq.gitlab.core.gitlab.GitLabExecutor;
import org.gitlab4j.api.GitLabApiException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class GitLabScannerController {

    @Resource
    private GitLabExecutor gitLabExecutor;

    /**
     *  案例: 127.0.0.1:9111/scanner?skipSuffix=pdf,xml,word,xlsx,xlx,wps,jar,doc,docx,zip,tar,gz,txt,png,jpg,jpeg,gif&excludeFileName=application-test,application-yufa,application-dev,application-pre&keyword=yuqing_ins
     * @param skipSuffix 跳过的文件后缀，多个用","分割
     * @param mustSuffix 必须扫描的文件后缀, 多个用","分割
     * @param excludeFileName 指定排除的文件名，多个用","分割， 判断方式包含的方式
     * @param keyword 搜素的关键字，本版本只支持 单个关键字
     * @return
     */
    @GetMapping(value = "/scanner")
    public String scanner(@RequestParam(name="skipSuffix", required = false) String skipSuffix,
        @RequestParam(name = "mustSuffix", required = false) String mustSuffix,
        @RequestParam(name = "excludeFileName", required = false) String excludeFileName,
        @RequestParam(name = "keyword") String keyword){

        FileRuleConfig fileRuleConfig = new FileRuleConfig();
        fileRuleConfig.setSkipFileSuffix(skipSuffix);
        fileRuleConfig.setMustFileSuffix(mustSuffix);
        fileRuleConfig.setKeyWords(keyword);
        fileRuleConfig.setExcludeFileName(excludeFileName);
        return gitLabExecutor.submit(fileRuleConfig);
    }

    @GetMapping(value = "/pull")
    public String pull() throws GitLabApiException {
        return gitLabExecutor.pullProject();
    }


}
