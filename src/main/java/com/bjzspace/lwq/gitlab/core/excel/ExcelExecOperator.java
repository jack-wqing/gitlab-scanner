package com.bjzspace.lwq.gitlab.core.excel;

import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.bjzspace.lwq.gitlab.entity.ProjectScannerResult;
import com.bjzspace.lwq.gitlab.entity.ProjectSimpleInfo;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class ExcelExecOperator{

    public static final Logger LOGGER = LoggerFactory.getLogger(ExcelExecOperator.class);

    public static void writeProjectExcel(String filePathName, String sheetName, List<ProjectSimpleInfo> projectInfos) {
        EasyExcel.write(filePathName, ProjectSimpleInfo.class).sheet(sheetName).doWrite(()-> projectInfos);
    }

    public static List<ProjectSimpleInfo> readProject(File file){
        List<ProjectSimpleInfo> data = Lists.newArrayList();
        EasyExcel.read(file, ProjectSimpleInfo.class, new PageReadListener<ProjectSimpleInfo>(dataList -> data.addAll(dataList))).sheet("project-list").doRead();
        return data;
    }

    public static synchronized void writeProjectScannerExcel(String filePathName, String sheetName, List<ProjectScannerResult> projectInfos) {
        LOGGER.info("current success:{}", JSONUtil.toJsonStr(projectInfos));
        File scannerFile = new File(filePathName);
        if(scannerFile.exists()){
            List<ProjectScannerResult> projectScannerResults = readProjectScannerExcel(scannerFile, sheetName);
            projectScannerResults.addAll(projectInfos);
            projectInfos = projectScannerResults;

        }
        EasyExcel.write(scannerFile, ProjectScannerResult.class).sheet(sheetName).doWrite(projectInfos);
    }

    public static List<ProjectScannerResult> readProjectScannerExcel(File file, String sheetName){
        List<ProjectScannerResult> data = Lists.newArrayList();
        EasyExcel.read(file, ProjectScannerResult.class, new PageReadListener<ProjectScannerResult>(dataList -> data.addAll(dataList))).sheet(sheetName).doRead();
        return data;
    }
}
