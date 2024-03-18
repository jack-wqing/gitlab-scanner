package com.bjzspace.lwq.gitlab.core.task;

import cn.hutool.json.JSONUtil;
import com.bjzspace.lwq.gitlab.core.scanner.DirScanner;
import com.bjzspace.lwq.gitlab.entity.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ScannerLooper extends BaseDaemonThread{

    public static final Logger LOGGER = LoggerFactory.getLogger(ScannerLooper.class);

    private static final AtomicLong index = new AtomicLong(0);

    @Resource
    private ScannerQueue scannerQueue;

    @Resource
    private ResultQueue resultQueue;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
        .availableProcessors() * 2);

    protected ScannerLooper() {
        super("ScannerLooper-" + index.getAndIncrement());
    }

    @PostConstruct
    public void start(){
        super.start();
    }
    @Override
    public void run() {
        while (true){
            try {
                ScannerEvent scannerEvent = scannerQueue.poolEvent();
                if(scannerEvent != null){
                    LOGGER.info("ScannerLooper event info: {}", JSONUtil.toJsonStr(scannerEvent));

                    DirScanner dirScanner = new DirScanner(scannerEvent.getFileRuleConfig());
                    File file = new File(scannerEvent.getProjectPath());
                    File[] dirs = file.listFiles(File::isDirectory);

                    ResultHandler.project(scannerEvent.getBatchId()).put(scannerEvent.getProject().getId(), scannerEvent.getProject());

                    AtomicInteger existCount = ResultHandler.projectBranchCount(scannerEvent.getBatchId()).get(scannerEvent.getProject().getId());
                    if(existCount == null){
                        ResultHandler.projectBranchCount(scannerEvent.getBatchId()).put(scannerEvent.getProject().getId(), new AtomicInteger(dirs == null ? 0 : dirs.length));
                    }
                    ConcurrentHashMap<String, ScanResult> projectResult =
                        ResultHandler.projectBranchResult(scannerEvent.getBatchId()).get(scannerEvent.getProject().getId());
                    if(projectResult == null){
                        ResultHandler.projectBranchResult(scannerEvent.getBatchId()).put(scannerEvent.getProject().getId(), new ConcurrentHashMap<>());
                    }
                    resultQueue.addEvent(scannerEvent.getBatchId());
                    if(dirs != null){
                        for (File dir : dirs) {
                            executorService.submit(() -> {
                                LOGGER.info("ScannerLooper scanner branch path:{}", dir.getAbsolutePath());
                                ScanResult scan = dirScanner.scan(dir.getAbsolutePath());
                                //放入执行结果
                                ConcurrentHashMap<String, ScanResult> branchResult =
                                    ResultHandler.projectBranchResult(scannerEvent.getBatchId()).get(scannerEvent.getProject().getId());
                                branchResult.put(scan.getRootPath(), scan);
                                //更新完成情况
                                AtomicInteger count = ResultHandler.projectBranchCount(scannerEvent.getBatchId()).get(scannerEvent.getProject().getId());
                                while (!count.compareAndSet(count.get(), count.get() -1)){
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }
}
