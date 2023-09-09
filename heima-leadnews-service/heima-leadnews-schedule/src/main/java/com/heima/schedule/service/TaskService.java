package com.heima.schedule.service;

import com.heima.model.schedule.dtos.Task;

public interface TaskService {

    /**
     * 添加延迟任务
     * @param task task dto
     * @return taskId
     */
    public long addTask(Task task);

    /**
     * 取消任务
     * @param taskId 任务id
     * @return 取消是否成功
     */
    public boolean cancelTask(long taskId);
}
