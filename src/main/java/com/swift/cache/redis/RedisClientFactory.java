/*
 * @(#)RedisClientFactory.java   1.0  2015年8月6日
 * 
 * Copyright (c)	2014-2020. All Rights Reserved.	GuangZhou hhmk Technology Company LTD.
 */
package com.swift.cache.redis;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.esotericsoftware.minlog.Log;
import com.swift.core.env.EnvDecode;
import com.swift.util.type.IpUtil;
import com.swift.util.type.TypeUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * redis数据访问层类
 * @author zhengjiajin
 * @version 1.0 2015年8月6日
 */
@Repository("redisClientFactory")
@Scope("singleton")
@Deprecated
public class RedisClientFactory {
    public static final Logger LOG = LoggerFactory.getLogger(RedisClientFactory.class);
    //切片连接池
    private JedisPool pool;
    private int maxActive=300;
    private int maxIdle=5;
    private long maxWait=30000;
    private boolean test=false;
    @Value("${redis.hosts:}")
    private String hosts="";
    @Value("${redis.password}")
    private String password="";
    
    private static ThreadLocal<Jedis> threadJedis = new ThreadLocal<Jedis>();  
    
    public static final String SUCCESS_STR="OK";
    
    @PostConstruct
    public void init(){
        if(TypeUtil.isNull(hosts)) {
            Log.warn("******没有配置REDIS地址******");
            return;
        }
        String pwd = EnvDecode.decode(password);
        // 池基本配置 
        JedisPoolConfig config = new JedisPoolConfig(); 
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle); 
        config.setMaxWaitMillis(maxWait); 
        config.setTestOnBorrow(test);
        if(hosts.indexOf(":")==-1){
            pool = new JedisPool(config,IpUtil.domainToIp(hosts),Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, pwd, Protocol.DEFAULT_DATABASE);
        }else{
            pool = new JedisPool(config,IpUtil.domainToIp(hosts.split(":")[0]),Integer.valueOf(hosts.split(":")[1]),Protocol.DEFAULT_TIMEOUT, pwd, Protocol.DEFAULT_DATABASE);
        }
    }
    
    public JedisPool getJedisPool(){
        return pool;
    }
    
    
    public Jedis getJedis(){
        if(pool==null) return null;
        Jedis jedis = null;
        try{
            if(threadJedis.get()!=null) {
                jedis = threadJedis.get();
            }else {
                jedis = pool.getResource();
                threadJedis.set(jedis);
            }
            if(!jedis.isConnected()){
                jedis.connect();
            }
        }catch(JedisConnectionException e){
            if(jedis!=null)jedis.close();
            throw new RuntimeException(e);
        }
        return jedis;
    }
    
    public void release(Jedis jedis){
        if(jedis!=null){
            jedis.close();
            threadJedis.remove();
        }
    }
    
    public void release(){
        Jedis jedis = threadJedis.get();
        if(jedis!=null){
            threadJedis.remove();
            jedis.close();
        }
    }

    public void close() throws IOException {
        if(pool!=null) pool.destroy();
    }
    
}
