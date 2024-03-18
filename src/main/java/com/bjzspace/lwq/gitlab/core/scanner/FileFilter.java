package com.bjzspace.lwq.gitlab.core.scanner;

import cn.hutool.core.io.file.FileNameUtil;
import com.bjzspace.lwq.gitlab.config.FileRuleConfig;
import com.bjzspace.lwq.gitlab.constant.Constants;
import com.bjzspace.lwq.gitlab.core.Filter;
import com.google.common.collect.Lists;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileFilter implements Filter {

    private FileRuleConfig fileRuleConfig;

    private List<String> skipSuffix = Lists.newArrayList();

    private List<String> mustSuffix = Lists.newArrayList();

    private List<String> excludeFileNameList = Lists.newArrayList();

    public FileFilter(FileRuleConfig fileRuleConfig) {
        this.fileRuleConfig = fileRuleConfig;
        String skipFileSuffix = fileRuleConfig.getSkipFileSuffix();
        if(StringUtils.hasText(skipFileSuffix)){
            skipSuffix.addAll(Arrays.stream(skipFileSuffix.split(Constants.COMMA)).collect(Collectors.toList()));
        }
        String mustFileSuffix = fileRuleConfig.getMustFileSuffix();
        if(StringUtils.hasText(mustFileSuffix)){
            mustSuffix.addAll(Arrays.stream(mustFileSuffix.split(Constants.COMMA)).collect(Collectors.toList()));
        }
        String excludeFileName = fileRuleConfig.getExcludeFileName();
        if(StringUtils.hasText(excludeFileName)){
            excludeFileNameList.addAll(Arrays.stream(excludeFileName.split(Constants.COMMA)).collect(Collectors.toList()));
        }
    }

    @Override
    public boolean skip(String name) {
        if(name.startsWith(Constants.DOT)){
            return false;
        }

        for (String excludeFileName : excludeFileNameList) {
            if(name.contains(excludeFileName)){
                return false;
            }
        }
        //排除没有文件后缀的文件
        String suffix = FileNameUtil.getSuffix(name);
        if(StringUtils.hasText(suffix)){
            if(mustSuffix.contains(suffix)){
                return true;
            }
            if(skipSuffix.contains(suffix)){
                return false;
            }
            return true;
        }
        return false;
    }

}
