package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

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


    /**
     * 按照类型和优先级拉取任务
     *
     * @param type     类型
     * @param priority 优先级
     * @return Task dto
     */
    @Override
    public Task pool(int type, int priority) {
        Task task = null;
        try {
            // 1. 从redis中拉取数据
            String key = type + "_" + priority;
            String taskJson = cacheService.lRightPop(ScheduleConstants.TOPIC + key);
            if (StringUtils.isNoneBlank(taskJson)) {
                task = JSON.parseObject(taskJson, Task.class);

                // 2. 更新数据库(删除任务， 更新任务日志)
                updataDB(task.getTaskId(), ScheduleConstants.EXECUTED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("拉取任务失败 taskId");
        }
        return task;
    }


    /**
     * 未来数据定时刷新，每分钟刷新一次
     */
    @Scheduled(cron = "0 */1 * * * ? ")
    public void refresh() {

        String token = cacheService.tryLock("FUTURE_TASK_SYNC", 1000 * 30);
        if (token != null) {

            log.info("未来数据定时刷新--定时任务");

            // 1. 获取所有未来数据的key的集合
            Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");

            // 2. 按照key和分值查询符合条件的数据
            for (String futureKey : futureKeys) { // future_100_50

                // 获取当前数据对应的topic key
                String topicKey = ScheduleConstants.TOPIC + futureKey.split(ScheduleConstants.FUTURE)[1];


                Set<String> tasks = cacheService.zRangeByScore(futureKey, 0, System.currentTimeMillis());

                // 3. 同步数据，使用redis管道
                if (!tasks.isEmpty()) {
                    cacheService.refreshWithPipeline(futureKey, topicKey, tasks);

                    log.info("成功将" + futureKey + "刷新到" + topicKey);
                }
            }
        }

    }


    /**
     * 数据库任务定时同步到redis
     */


    @PostConstruct
    @Scheduled(cron = "0 */5 * * * ?")
    public void reloadData() {

        // 清理缓存中的数据
        clearCache();

        // 查询符合条件的任务
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, ScheduleConstants.NEXT_SCHEDULE_TIME);
        List<Taskinfo> taskinfos = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().lt(Taskinfo::getExecuteTime, calendar.getTime()));

        // 把任务添加到redis
        if (taskinfos != null && taskinfos.size() > 0) {
            for (Taskinfo taskinfo : taskinfos) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo, task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                addTask2Cache(task);
            }
        }
        log.info("数据库任务同步到Redis");
    }

    public void clearCache() {
        Set<String> topicKeys = cacheService.scan(ScheduleConstants.TOPIC + "*");
        Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
        cacheService.delete(topicKeys);
        cacheService.delete(futureKeys);
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
            e.printStackTrace();
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
