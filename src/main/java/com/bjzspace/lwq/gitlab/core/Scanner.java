package com.bjzspace.lwq.gitlab.core;

import com.bjzspace.lwq.gitlab.entity.ScanResult;

public interface Scanner {

    ScanResult scan(String path);

}
