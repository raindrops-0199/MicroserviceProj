package com.heima.apis.schedule;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.schedule.dtos.Task;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("leadnews-schedule")
public interface IScheduleClient {
    /**
     * 添加延迟任务
     * @param task task dto
     * @return taskId
     */
    @PostMapping("/api/v1/task/add")
    public ResponseResult addTask(@RequestBody Task task);

    /**
     * 取消任务
     * @param taskId 任务id
     * @return 取消是否成功
     */
    @GetMapping("/api/v1/task/{taskId}")
    public ResponseResult cancelTask(@PathVariable long taskId);

    /**
     * 按照类型和优先级拉取任务
     * @param type 类型
     * @param priority 优先级
     * @return Task dto
     */
    @GetMapping("/api/v1/task/{type}/{priority}")
    public ResponseResult pool(@PathVariable int type, @PathVariable int priority);
}
