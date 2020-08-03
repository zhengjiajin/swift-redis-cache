/*
 * @(#)TaskAop.java   1.0  2018年5月31日
 * 
 * Copyright (c)	2014-2020. All Rights Reserved.	GuangZhou hhmk Technology Company LTD.
 */
package com.swift.cache.redis;

import java.lang.reflect.Method;
import java.util.Date;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;

import com.swift.util.bean.AnnotationUtil;
import com.swift.util.date.DateUtil;
import com.swift.util.type.TypeUtil;

import redis.clients.jedis.Jedis;

/**
 * 添加说明
 * 
 * @author zhengjiajin
 * @version 1.0 2018年5月31日
 */
@Aspect
@Component
@Order(1)
public class TaskAop {

    private static final Logger log = LoggerFactory.getLogger(TaskAop.class);

    public static final String FILTER_STR = "@annotation(org.springframework.scheduling.annotation.Scheduled) ";

    @Autowired
    private RedisClientFactory redisClientFactory;

    private static final String REDIS_KEY = "TaskAop:";

    private static SimpleTriggerContext triggerContext = new SimpleTriggerContext();

    @Around(TaskAop.FILTER_STR)
    public void around(ProceedingJoinPoint pjp) throws Throwable {
        Object target = pjp.getTarget();
        Method currentMethod = getMethod(pjp);
        String key = target.getClass().getName() + currentMethod.getName() + currentMethod.getParameterCount();
        Scheduled scheduled = AnnotationUtil.getAnnotation(currentMethod, Scheduled.class);
        int seconds = 0;
        if (TypeUtil.isNotNull(scheduled.cron())) {
            CronTrigger cronTrigger = new CronTrigger(scheduled.cron());
            Date nextExec = cronTrigger.nextExecutionTime(triggerContext);
            seconds = TypeUtil.toInt((nextExec.getTime() - System.currentTimeMillis()) / 1000);
            log.info(key + "当前运行时间:" + DateUtil.formatDate(new Date()) + ";下次运行时间:" + DateUtil.formatDate(nextExec));
        }
        if(scheduled.fixedDelay()>0){
            seconds=TypeUtil.toInt(scheduled.fixedDelay()/1000);
        }
        
        if(TypeUtil.isNotNull(scheduled.fixedDelayString())) {
            seconds=TypeUtil.toInt(scheduled.fixedDelayString())/1000;
        }
        
        if(scheduled.fixedRate()>0){
            seconds=TypeUtil.toInt(scheduled.fixedRate()/1000);
        }
        
        if(TypeUtil.isNotNull(scheduled.fixedRateString())) {
            seconds=TypeUtil.toInt(scheduled.fixedRateString())/1000;
        }
       
        if (!isThisJob(key, seconds)) {
            log.info("不在此机器执行:" + key);
            return;
        }
        pjp.proceed(pjp.getArgs());
    }

    private Method getMethod(ProceedingJoinPoint pjp) throws NoSuchMethodException {
        Signature sig = pjp.getSignature();
        MethodSignature msig = null;
        if (!(sig instanceof MethodSignature)) {
            throw new IllegalArgumentException("该注解只能用于方法");
        }
        msig = (MethodSignature) sig;
        Method currentMethod = pjp.getTarget().getClass().getMethod(msig.getName(), msig.getParameterTypes());
        return currentMethod;
    }

    private boolean isThisJob(String key, int seconds) {
        boolean isThisJob = false;
        Jedis jedis = redisClientFactory.getJedis();
        if(jedis==null) return true;
        if (seconds > 1) seconds = seconds - 1;// 系统处理redis更改操作时间
        String str = jedis.set(redisKey(key), String.valueOf(seconds), "NX", "EX", seconds);
        if (TypeUtil.isNotNull(str) && str.equals("OK")) {
            isThisJob = true;
        } 
        redisClientFactory.release(jedis);
        return isThisJob;
    }

    private String redisKey(String key) {
        return REDIS_KEY + key;
    }

}
