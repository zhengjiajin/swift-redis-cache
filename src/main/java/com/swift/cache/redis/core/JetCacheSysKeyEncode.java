/*
 * @(#)JetCacheKeyEncode.java   1.0  2019年7月12日
 * 
 * Copyright (c)	2014-2020. All Rights Reserved.	GuangZhou hhmk Technology Company LTD.
 */
package com.swift.cache.redis.core;

import java.util.function.Function;

import com.alibaba.fastjson.JSON;

/**
 * 添加说明 
 * @author zhengjiajin
 * @version 1.0 2019年7月12日
 */
public class JetCacheSysKeyEncode implements Function<Object, Object> {

    private final static String SYS_SPILT="_.";
    
    private String sysId="";
    
    public JetCacheSysKeyEncode(String sysId) {
        if(sysId!=null) this.sysId=sysId;
    }
    
    /** 
     * @see java.util.function.Function#apply(java.lang.Object)
     */
    @Override
    public Object apply(Object originalKey) {
        if (originalKey == null) {
            return null;
        }
        if (originalKey instanceof String) {
            return sysId+SYS_SPILT+originalKey;
        }
        return sysId+SYS_SPILT+JSON.toJSONString(originalKey);
    }

}
