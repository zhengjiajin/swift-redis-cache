/*
 * @(#)RedisReleaseFilter.java   1.0  2018年6月4日
 * 
 * Copyright (c)	2014-2020. All Rights Reserved.	GuangZhou hhmk Technology Company LTD.
 */
package com.swift.cache.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.swift.core.filter.EndFilter;

/**
 * 添加说明 
 * @author zhengjiajin
 * @version 1.0 2018年6月4日
 */
@Component
@Order(Integer.MAX_VALUE)
public class RedisReleaseFilter implements EndFilter {

    @Autowired
    private RedisClientFactory redisClientFactory;
    /** 
     * @see com.swift.core.filter.EndFilter#end()
     */
    @Override
    public void end() {
        redisClientFactory.release();
    }

}
