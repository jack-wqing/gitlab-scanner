package com.bjzspace.lwq.gitlab.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ScanResult {

    private boolean find;
    private long lineNumber;
    private String fileName;
    private String rootPath;

    public static ScanResult newTrueInstance(long lineNumber,String fileName){
        return new ScanResult(true, lineNumber, fileName, null);
    }
    public static ScanResult newTrueInstance(long lineNumber,String fileName, String rootPath){
        return new ScanResult(true, lineNumber, fileName, rootPath);
    }

    public static ScanResult newFalseInstance(String fileName, String rootPath){
        return new ScanResult(false, -1, fileName, rootPath);
    }
    public static ScanResult newFalseInstance(){
        return new ScanResult(false, -1, null, null);
    }


}
