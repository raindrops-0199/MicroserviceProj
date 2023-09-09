package com.heima.schedule.test;

import com.alibaba.fastjson.JSON;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.ScheduleApplication;
import com.sun.istack.internal.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;
import java.util.Set;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class RedisTest {

    @Autowired
    private CacheService cacheService;

    @Test
    public void testList() {

        // 在list的左边添加元素
        cacheService.lLeftPush("list_01", "hello, list");

        // 在list的右边获取元素，并删除
        String list01 = cacheService.lRightPop("list_01");
        System.out.println(list01);
    }

    @Test
    public void testZset() {

        // 添加数据到zset中 分值
//        cacheService.zAdd("zset_01", "hello, zset1", 1000);
//        cacheService.zAdd("zset_01", "hello, zset2", 8888);
//        cacheService.zAdd("zset_01", "hello, zset3", 0);
//        cacheService.zAdd("zset_01", "hello, zset4", 20);

        // 按照分值获取数据
        Set<String> zset = cacheService.zRangeByScore("zset_01", 0, 1000);
        System.out.println(zset);
    }

    // 耗时7469ms
    @Test
    public  void testPiple1(){
        long start =System.currentTimeMillis();
        for (int i = 0; i <10000 ; i++) {
            Task task = new Task();
            task.setTaskType(1001);
            task.setPriority(1);
            task.setExecuteTime(new Date().getTime());
            cacheService.lLeftPush("1001_1", JSON.toJSONString(task));
        }
        System.out.println("耗时"+(System.currentTimeMillis()- start));
    }


    // 耗时1887ms
    @Test
    public void testPiple2(){
        long start  = System.currentTimeMillis();
        //使用管道技术
        List<Object> objectList = cacheService.getstringRedisTemplate().executePipelined(new RedisCallback<Object>() {
            @Nullable
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                for (int i = 0; i <10000 ; i++) {
                    Task task = new Task();
                    task.setTaskType(1001);
                    task.setPriority(1);
                    task.setExecuteTime(new Date().getTime());
                    redisConnection.lPush("1001_1".getBytes(), JSON.toJSONString(task).getBytes());
                }
                return null;
            }
        });
        System.out.println("使用管道技术执行10000次自增操作共耗时:"+(System.currentTimeMillis()-start)+"毫秒");
    }

}
