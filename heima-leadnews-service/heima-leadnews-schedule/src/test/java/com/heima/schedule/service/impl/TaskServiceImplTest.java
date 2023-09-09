package com.heima.schedule.service.impl;

import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.ScheduleApplication;
import com.heima.schedule.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
class TaskServiceImplTest {

    @Autowired
    private TaskService taskService;

    @Test
    void addTask() {
        Task task = new Task();
        task.setTaskType(100);
        task.setPriority(50);
        task.setParameters("task_tes".getBytes());

        // 立即执行
        task.setExecuteTime(new Date().getTime());

        // 延迟执行, 加入zset
        //task.setExecuteTime(System.currentTimeMillis() + 1000 * 60 * 4);

        // 延迟执行，超过预设时间，不存入Redis
        //task.setExecuteTime(System.currentTimeMillis() + 1000 * 60 * 6);

        long taskId = taskService.addTask(task);
        System.out.println(taskId);
    }

    @Test
    void cancelTest() {
        long taskId = 1700433490000084993L;
        boolean res = taskService.cancelTask(taskId);
        assertTrue(res);
    }
}