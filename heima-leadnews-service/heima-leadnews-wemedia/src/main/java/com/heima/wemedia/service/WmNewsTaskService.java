package com.heima.wemedia.service;

import java.util.Date;

public interface WmNewsTaskService {

    /**
     * 添加任务到延迟队列中
     * @param id 文章id
     * @param publishTime 文章发布时间(作为任务执行时间)
     */
    public void addNews2Task(Integer id, Date publishTime);

    /**
     * 消费任务，审核文章
     */
    public void scanNewsByTask();
}
