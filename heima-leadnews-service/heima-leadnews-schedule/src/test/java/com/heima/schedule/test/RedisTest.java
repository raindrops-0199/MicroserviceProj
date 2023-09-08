package com.heima.schedule.test;

import com.heima.common.redis.CacheService;
import com.heima.schedule.ScheduleApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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

}
