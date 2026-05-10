package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.entity.Shop;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData), time, unit);
    }
    public <R ,ID> R queryWithPassThrough(String keyPrefix , ID id, Class<R> type, Function<ID,R> dbFallbakc,Long time, TimeUnit unit) {
        String key = keyPrefix+id;
        String json= stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if(StringUtils.isNotBlank(json)){
            return JSONUtil.toBean(json,type);
        }
        if(json!=null){
            return null;
        }
      //从数据库中查
        R r  = dbFallbakc.apply(id);
        //不存在返回null
        if(r == null){
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存在 写入redis缓存
        //返回数据库数据
        this.set(key,r,time, unit);

        return r;
    }
    public <R,ID>R queryWithMutex(String keyPrefix, ID id,Class<R> type,Function<ID,R> dbFallbakc,Long time, TimeUnit unit){
        String key = keyPrefix+id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if(StringUtils.isNotBlank(json)){
            return JSONUtil.toBean(json,type);
        }
        if(json!=null){
            return null;
        }
        //判断是否有锁
        String LockKey= LOCK_SHOP_KEY+id;
        R r = null;
        try {
            boolean isLock =  tryLock(LockKey);
            if(!isLock){
                //休眠
                Thread.sleep(50);

                return queryWithMutex(keyPrefix,id,type,dbFallbakc,time, unit);
            }

            //不存在从数据库查询
            r = dbFallbakc.apply(id);
            //不存在返回null
            if(r == null){
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r),time, unit);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            unlock(LockKey);
        }

        //返回数据库数据
        return r;
    }
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool ( 10) ;
    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type,Function<ID,R> dbFallbakc,Long time, TimeUnit unit) {
        String key = keyPrefix+id;
        String json = stringRedisTemplate.opsForValue().get(keyPrefix+id);
        //判断是否存在
        if(StringUtils.isBlank(json)) {
            return null;
        }
        //命中 把json序列化为对象
        RedisData redisData = JSONUtil.toBean(json,RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //缓存判断是否过期
        //未过期
        if(expireTime.isAfter(LocalDateTime.now())){
            return r;
        }
        //过期
        //缓存重建
        //获取互斥锁
        String LockKey = LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(LockKey);
        if(isLock){
            //开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                //重建缓存
                try {
                    R r1 = dbFallbakc.apply(id);
                    this.setWithLogicalExpire(key,r1,time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    //释放锁
                    unlock(LockKey);
                }
            });
        }
        return r;
    }
    private  boolean tryLock(String key){
        boolean flag =stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
