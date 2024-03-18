package com.bjzspace.lwq.gitlab.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ProjectScannerResult {
    @ExcelProperty("项目id")
    private Long id;
    @ExcelProperty("项目名")
    private String name;
    @ExcelProperty("项目所属人")
    private String owner;
    /**
     * 分支信息及包含分支关键词
     */
    @ExcelProperty("分支信息")
    private String branchInfo;
    /**
     * 是否包含关键词 是/否
     */
    @ExcelProperty("是否关键词")
    private String containKeyWords;
    @ExcelProperty("项目地址")
    private String httpUrl;

}
