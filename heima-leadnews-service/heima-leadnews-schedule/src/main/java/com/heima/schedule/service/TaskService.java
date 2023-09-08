package com.heima.schedule.service;

import com.heima.model.schedule.dtos.Task;

public interface TaskService {

    /**
     * 添加延迟任务
     * @param task task dto
     * @return taskId
     */
    public long addTask(Task task);
}
