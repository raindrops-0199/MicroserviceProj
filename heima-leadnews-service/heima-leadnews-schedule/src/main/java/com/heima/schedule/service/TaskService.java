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

    /**
     * 按照类型和优先级拉取任务
     * @param type 类型
     * @param priority 优先级
     * @return Task dto
     */
    public Task pool(int type, int priority);
}
