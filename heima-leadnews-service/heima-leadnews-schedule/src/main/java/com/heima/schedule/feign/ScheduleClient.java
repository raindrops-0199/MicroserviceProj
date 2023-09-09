package com.heima.schedule.feign;

import com.heima.apis.schedule.IScheduleClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class ScheduleClient implements IScheduleClient {

    @Autowired
    private TaskService taskService;


    /**
     * 添加延迟任务
     *
     * @param task task dto
     * @return taskId
     */
    @Override
    @PostMapping("/api/v1/task/add")
    public ResponseResult addTask(@RequestBody Task task) {
        long taskId = taskService.addTask(task);
        return ResponseResult.okResult(taskId);
    }

    /**
     * 取消任务
     *
     * @param taskId 任务id
     * @return 取消是否成功
     */
    @Override
    @GetMapping("/api/v1/task/{taskId}")
    public ResponseResult cancelTask(@PathVariable long taskId) {
        boolean res = taskService.cancelTask(taskId);
        return ResponseResult.okResult(res);
    }

    /**
     * 按照类型和优先级拉取任务
     *
     * @param type     类型
     * @param priority 优先级
     * @return Task dto
     */
    @Override
    @GetMapping("/api/v1/task/{type}/{priority}")
    public ResponseResult pool(@PathVariable int type, @PathVariable int priority) {
        Task task = taskService.pool(type, priority);
        return ResponseResult.okResult(task);
    }
}
