package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

@Service
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskinfoMapper taskinfoMapper;

    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;

    @Autowired
    private CacheService cacheService;

    /**
     * 添加延迟任务
     *
     * @param task task dto
     * @return taskId
     */
    @Override
    public long addTask(Task task) {

        // 1. 添加任务到数据库中
        boolean success = addTask2DB(task);

        if (success) {
            // 2. 添加任务到Redis中
            addTask2Cache(task);
        }
        return task.getTaskId();
    }

    /**
     * 取消任务
     *
     * @param taskId 任务id
     * @return 取消是否成功
     */
    @Override
    public boolean cancelTask(long taskId) {
        // 1. 删除任务，更新任务日志
        Task task = updataDB(taskId, ScheduleConstants.CANCELLED);

        // 2. 删除Redis数据
        if (task == null) {
            return false;
        }
        String key = task.getTaskType() + "_" + task.getPriority();
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            cacheService.lRemove(ScheduleConstants.TOPIC + key, 0, JSON.toJSONString(task));
        } else {
            cacheService.zRemove(ScheduleConstants.FUTURE + key, JSON.toJSONString(task));
        }

        return true;
    }

    private Task updataDB(long taskId, int status) {
        Task task = null;
        try {
            // delete task info
            taskinfoMapper.deleteById(taskId);

            // update task info log
            TaskinfoLogs taskinfoLog = taskinfoLogsMapper.selectById(taskId);
            taskinfoLog.setStatus(status);
            taskinfoLogsMapper.updateById(taskinfoLog);

            task = new Task();
            BeanUtils.copyProperties(taskinfoLog, task);
            task.setExecuteTime(taskinfoLog.getExecuteTime().getTime());

        } catch (Exception e) {
            log.error("task cancel exception taskId={}", taskId);
        }
        return task;
    }

    /**
     * 把任务添加到Redis中
     *
     * @param task Task dto
     */
    private void addTask2Cache(Task task) {

        String key = task.getTaskType() + "_" + task.getPriority();

        // 获取5分钟预设时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        long nextScheduleTime = calendar.getTimeInMillis();

        long time = System.currentTimeMillis();
        // 2.1 如果任务执行时间 <= 当前时间，存入list
        if (task.getExecuteTime() <= time) {
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        } else if (task.getExecuteTime() <= nextScheduleTime) {
            // 2.2 任务执行时间 > 当前时间 && <= 预设时间(未来5分钟)，存入zset
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());
        }
    }

    /**
     * 添加任务到数据库
     *
     * @param task task
     * @return success-true fail-false
     */
    private boolean addTask2DB(Task task) {

        boolean flag = false;

        try {
            // 1. 保存任务表
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);

            // 设置taskId
            task.setTaskId(taskinfo.getTaskId());

            // 2. 保存任务日志表
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            taskinfoLogs.setVersion(1);
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogsMapper.insert(taskinfoLogs);

            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return flag;
    }
}
