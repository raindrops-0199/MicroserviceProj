package com.heima.wemedia.service;

import java.lang.reflect.InvocationTargetException;

public interface WmNewsAutoScanService {

    /**
     * 自媒体文章审核
     * @param id 自媒体文章id
     */
    public void autoScanWmNews(Integer id);
}
