package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMESTAMP = 1775001600L;
    private StringRedisTemplate stringRedisTemplate;
    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public  Long nextId(String keyProfex) {
        //生成时间戳
      LocalDateTime now = LocalDateTime.now();
      long timestamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
      String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //生成序列号
        Long count = stringRedisTemplate.opsForValue().increment("icr"+keyProfex+":"+date);
        return timestamp << 32 | count;
    }


}
