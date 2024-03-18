package com.bjzspace.lwq.gitlab.core.scanner;

import com.bjzspace.lwq.gitlab.config.FileRuleConfig;
import com.bjzspace.lwq.gitlab.constant.Constants;
import com.bjzspace.lwq.gitlab.core.Filter;
import com.bjzspace.lwq.gitlab.core.Scanner;
import com.bjzspace.lwq.gitlab.entity.ScanResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 对文件夹的文件目录进行扫描
 */
public class DirScanner implements Scanner {

    private FileScanner fileScanner;

    private Filter filter;

    public DirScanner(FileRuleConfig fileRuleConfig) {
        this.fileScanner = new FileScanner(fileRuleConfig);
        this.filter = new FileFilter(fileRuleConfig);
    }

    @Override
    public ScanResult scan(String dirPath) {
        Path startingDir = Paths.get(dirPath);
        ScanResult scanResult = ScanResult.newFalseInstance();
        String[] split = dirPath.split(File.separator);
        scanResult.setRootPath(split[split.length-1]);
        try {
            Files.walkFileTree(startingDir, new FindJavaVisitor(this.fileScanner, this.filter, scanResult));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scanResult;
    }

    private class FindJavaVisitor extends SimpleFileVisitor<Path> {
        private FileScanner fileScanner;
        private Filter filter;
        private ScanResult scanResult;
        public FindJavaVisitor(FileScanner fileScanner, Filter filter, ScanResult scanResult) {
            this.fileScanner = fileScanner;
            this.filter = filter;
            this.scanResult = scanResult;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String name = dir.getFileName().toFile().getName();
            if(name.startsWith(Constants.DOT)){
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs){
            if(filter.skip(file.getFileName().toFile().getName())){
                ScanResult result = fileScanner.scan(file.toFile().getAbsolutePath());
                if(result.isFind()){
                    scanResult.setFind(result.isFind());
                    scanResult.setLineNumber(result.getLineNumber());
                    scanResult.setFileName(result.getFileName());
                    return FileVisitResult.TERMINATE;
                }
            }
            return FileVisitResult.CONTINUE;
        }
    }

}
