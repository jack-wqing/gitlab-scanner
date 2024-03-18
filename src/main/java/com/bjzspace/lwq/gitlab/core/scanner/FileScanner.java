package com.bjzspace.lwq.gitlab.core.scanner;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.CharsetUtil;
import com.bjzspace.lwq.gitlab.config.FileRuleConfig;
import com.bjzspace.lwq.gitlab.core.Scanner;
import com.bjzspace.lwq.gitlab.entity.ScanResult;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class FileScanner implements Scanner {

    public static final Logger LOGGER = LoggerFactory.getLogger(FileScanner.class);

    private FileRuleConfig fileRuleConfig;

    List<Charset> charsetList = Arrays.asList(Charset.forName(CharsetUtil.UTF_8), Charset.forName(CharsetUtil.GBK), Charset.forName(CharsetUtil.ISO_8859_1),Charset.forName(CharsetUtil.defaultCharsetName()));


    public FileScanner(FileRuleConfig fileRuleConfig) {
        this.fileRuleConfig = fileRuleConfig;
    }
    
    @Override
    public ScanResult scan(String path) {
        for (Charset set : charsetList) {
            try {
                List<String> list = Files.readAllLines(Paths.get(path), set);
                if(CollectionUtils.isEmpty(list)){
                    return ScanResult.newFalseInstance();
                }
                for (int i = 0; i < list.size(); i++) {
                    String line = list.get(i);
                    if(line.contains(fileRuleConfig.getKeyWords())){
                        return ScanResult.newTrueInstance(i + 1, FileNameUtil.getName(path));
                    }
                }
                break;
            } catch (IOException e) {
                // nothing
                LOGGER.info("FileScanner error, path:{}, charset:{}", path, set.name());
            }
        }
        return ScanResult.newFalseInstance();

    }




}
