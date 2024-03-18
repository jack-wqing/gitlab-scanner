package com.bjzspace.lwq.gitlab.core;

import cn.hutool.core.io.file.FileNameUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Test {

    @org.junit.Test
    public void suffix(){
        String suffix = FileNameUtil.getSuffix(".aaa");
    }

    @org.junit.Test
    public void testReplace(){
        String branchName = "   a   v  ";
        branchName = branchName.replaceAll(" ", "=");
        System.out.println(branchName);
    }

    @org.junit.Test
    public void testDis() throws URISyntaxException, IOException {
        List<String> list1 = Files.readAllLines(Paths.get("/Users/qing/1.txt"));

        List<String> list2 = Files.readAllLines(Paths.get("/Users/qing/2.txt"));

        for (String line: list2) {
            if(!list1.contains(line)){
                System.out.println(line);
            }
        }
    }

}
