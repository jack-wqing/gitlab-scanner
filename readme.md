### 一、功能说明
    对gitlab进行关键字的代码分支查找
### 二、支持的功能
    1.对所有项目，或者部分项目进行扫描
    2.对项目所有分支或者按提交时间倒叙指定分支数扫描
    3.支持跳过指定的后缀，支持必须扫描的文件后缀
    4.spring-boot web项目，支持web部署,执行并发多批次查询
    
### 三、使用说明
#### 1.环境需求
    1.需要执行程序的主机安装git
    2.需要jdk8及以上环境
#### 2.程序说明
    1.配置文件 application.yml
      gitlab:
        gitLabDomain: "https://git.ty.ink"   // 指定gilab的访问地址
        accessToken: "accessToken"           // 作为访问的token
        workSpace: "/data2/gitlab/"          // 程序执行的家目录 ：比如：程序git下载的代码，扫描之后的结果文件，指定扫描项目文件放置目录
        projectList: "projects.xlsx"         // 指定扫描的项目集合，可以通过启动项目，通过接口访问下载，下载之后放置在workSpace 目录中
        scanBranchCount: 3                   // 指定每个项目扫描多个少分支，是提交时间倒叙选择
        deleteOkProject: true                // 指定扫描完成之后，是否删除下载的项目
        
      spring:
        application:
        name: gilab-scanner
        server:
        port: 9111
#### 2.项目启动
    就是一个普通的spring boot 项目，按spring boot运行的方式运行
#### 3.接口说明，该程序只有两个接口
    接口1: 查看所有得项目[如果公司项目太多，可能超时]，修改接口分页，结果保存在gitlab.workSpace目录中，文件名为:gitlab.projectList 指定的名字
          例子: http://127.0.0.1:8080/pull
    接口2: 扫描接口，如果在gitlab.workSpace放置了名为gitlab.projectList项目xlsx文件，则只会扫描包含的项目，否则扫描整个项目
          例子: http://127.0.0.1:9111/scanner?skipSuffix=pdf,xml,word,xlsx,xlx,wps,jar,doc,docx,zip,tar,gz,txt,png,jpg,jpeg,gif&keyword=yuqing_ins

