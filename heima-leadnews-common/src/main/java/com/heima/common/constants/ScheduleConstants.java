package com.heima.common.constants;

public class ScheduleConstants {

    //task状态
    public static final int SCHEDULED=0;   //初始化状态

    public static final int EXECUTED=1;       //已执行状态

    public static final int CANCELLED=2;   //已取消状态

    public static final String FUTURE="future_";   //未来数据key前缀

    public static final String TOPIC="topic_";     //当前数据key前缀

    public static final int NEXT_SCHEDULE_TIME = 5;  //设置为5分钟，5分钟后的任务不存入缓存，直接存入数据库
}