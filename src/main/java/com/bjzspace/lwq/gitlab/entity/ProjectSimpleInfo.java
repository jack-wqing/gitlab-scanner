package com.bjzspace.lwq.gitlab.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ProjectSimpleInfo {
    @ExcelProperty("项目id")
    private Long id;
    @ExcelProperty("项目名")
    private String name;
    @ExcelProperty("项目所属人")
    private String owner;
    @ExcelProperty("项目可见性")
    private String visibility;
    @ExcelProperty("项目地址")
    private String httpUrl;

}
