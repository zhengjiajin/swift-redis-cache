/*
 * @(#)JetCacheConfig.java   1.0  2018年4月27日
 * 
 * Copyright (c)	2014-2020. All Rights Reserved.	GuangZhou YY Technology Company LTD.
 */
package com.swift.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.alicp.jetcache.CacheBuilder;
import com.alicp.jetcache.anno.CacheConsts;
import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.alicp.jetcache.anno.support.GlobalCacheConfig;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.alicp.jetcache.embedded.LinkedHashMapCacheBuilder;
import com.alicp.jetcache.redis.springdata.RedisSpringDataCacheBuilder;
import com.alicp.jetcache.support.FastjsonKeyConvertor;
import com.alicp.jetcache.support.KryoValueDecoder;
import com.alicp.jetcache.support.KryoValueEncoder;
import com.swift.cache.redis.core.JetCacheSysKeyEncode;
import com.swift.cache.redis.core.SpringRedisSysKeyEncode;
import com.swift.core.env.EnvDecode;
import com.swift.util.type.IpUtil;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * 
 * @author 郑家锦
 * @version 1.0 2018年4月27日
 */
@Configuration
@EnableMethodCache(basePackages = {"com.swift","com.hhmk","com.test"})
@EnableCreateCacheAnnotation
public class JetCacheConfig {
    
    @Value("${sysId}")
    private String sysId;
    
    private int maxActive=300;
    private int maxIdle=5;
    private long maxWait=30000;
    private boolean test=false;
    @Value("${redis.hosts:}")
    private String hosts="";
    @Value("${redis.password}")
    private String password="";

    @Bean
    public SpringConfigProvider springConfigProvider() {
        return new SpringConfigProvider();
    }
    
    @Bean
    public GlobalCacheConfig config(RedisConnectionFactory redisConnectionFactory) {
        Map<String, CacheBuilder> localBuilders = new HashMap<String, CacheBuilder>();
        CacheBuilder localBuilder = LinkedHashMapCacheBuilder.createLinkedHashMapCacheBuilder().keyConvertor(FastjsonKeyConvertor.INSTANCE);
        localBuilders.put(CacheConsts.DEFAULT_AREA, localBuilder);
        Map<String, CacheBuilder> remoteBuilders = new HashMap<String, CacheBuilder>();
        CacheBuilder remoteCacheBuilder = RedisSpringDataCacheBuilder.createBuilder()
                .keyConvertor(new JetCacheSysKeyEncode(sysId))
                .valueEncoder(KryoValueEncoder.INSTANCE)
                .valueDecoder(KryoValueDecoder.INSTANCE)
                .expireAfterWrite(7, TimeUnit.DAYS)
                .connectionFactory(redisConnectionFactory);
        remoteBuilders.put(CacheConsts.DEFAULT_AREA, remoteCacheBuilder);
        GlobalCacheConfig globalCacheConfig = new GlobalCacheConfig();
        globalCacheConfig.setLocalCacheBuilders(localBuilders);
        globalCacheConfig.setRemoteCacheBuilders(remoteBuilders);
        globalCacheConfig.setStatIntervalMinutes(15);
        globalCacheConfig.setAreaInCacheName(false);
        return globalCacheConfig;
    }
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMaxWaitMillis(maxWait);
        poolConfig.setMinIdle(3);
        poolConfig.setTestOnBorrow(test);
        poolConfig.setTestOnReturn(test);
        poolConfig.setTestWhileIdle(test);
        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .usePooling().poolConfig(poolConfig).and().readTimeout(Duration.ofMillis(Protocol.DEFAULT_TIMEOUT)).build();
        if(hosts.indexOf(",")>-1) {
            RedisClusterConfiguration redisConfig = new RedisClusterConfiguration();
            for(String host:hosts.split(",")) {
                if(host.indexOf(":")==-1){
                    redisConfig.clusterNode(IpUtil.domainToIp(host), Protocol.DEFAULT_PORT);
                }else{
                    redisConfig.clusterNode(IpUtil.domainToIp(host.split(":")[0]), Integer.valueOf(host.split(":")[1]));
                }
            }
            redisConfig.setPassword(EnvDecode.decode(password));
            return new JedisConnectionFactory(redisConfig,clientConfig);
        } else {
            RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
            if(hosts.indexOf(":")==-1){
                redisConfig.setHostName(IpUtil.domainToIp(hosts));
                redisConfig.setPort(Protocol.DEFAULT_PORT);
            }else{
                redisConfig.setHostName(IpUtil.domainToIp(hosts.split(":")[0]));
                redisConfig.setPort(Integer.valueOf(hosts.split(":")[1]));
            }
            redisConfig.setPassword(EnvDecode.decode(password));
            redisConfig.setDatabase(Protocol.DEFAULT_DATABASE);
            return new JedisConnectionFactory(redisConfig,clientConfig);
        }
       
    }
    
    @Bean
    public RedisTemplate<String,String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String,String> redisTemplate = new RedisTemplate<String,String>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new SpringRedisSysKeyEncode(sysId));
        //redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        //redisTemplate.setEnableTransactionSupport(true);
        return redisTemplate;
    }

}
