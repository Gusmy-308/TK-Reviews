package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import jakarta.annotation.Resource;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
        @Resource
        private CacheClient cacheClient;
        @Resource
        private StringRedisTemplate stringRedisTemplate;
        @Override
        public Object queryById(Long id) {
            //缓存穿透
            //Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,id2->getById(id2),CACHE_SHOP_TTL,TimeUnit.MINUTES);
            //互斥锁解决缓存击穿
            //Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,id2->getById(id2),CACHE_SHOP_TTL,TimeUnit.MINUTES);
            //逻辑过期解决缓存击穿
           Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,id2->getById(id2),CACHE_SHOP_TTL,TimeUnit.MINUTES);
            if (shop==null){
                return Result.fail("店铺不存在");
            }
            return Result.ok(shop);
        }
//        private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool ( 10) ;
    /*public Shop queryWithLogicalExpire(Long id) {
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        //判断是否存在
        if(StringUtils.isBlank(shopJson)) {
            return null;
        }
        //命中 把json序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson,RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(),Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //缓存判断是否过期
        //未过期
        if(expireTime.isAfter(LocalDateTime.now())){
            return shop;
        }
        //过期
        //缓存重建
        //获取互斥锁
        String LockKey = "lock:shop:"+id;
        boolean isLock = tryLock(LockKey);
        if(isLock){
            //开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                //重建缓存
                try {
                    this.saveShopTORedis(id,20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    //释放锁
                    unlock(LockKey);
                }
            });
        }
        return shop;
    }*/
    /*public Shop queryWithMutex(Long id){
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        //判断是否存在
        if(StringUtils.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson,Shop.class);
        }
        if(shopJson!=null){
            return null;
        }
        //判断是否有锁
        String LockKey= "lock:shop:"+id;
        Shop shop = null;
        try {
            boolean isLock =  tryLock(LockKey);
            if(!isLock){
                //休眠
                Thread.sleep(50);

                return queryWithMutex(id);
            }

            //不存在从数据库查询
            shop = this.getById(id);

            if(shop == null){
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //不存在返回null
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            unlock(LockKey);
        }

        //返回数据库数据
        return shop;
    }*/
       /* public Shop queryWithPassThrough(Long id) {
            String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
            //判断是否存在
            if(StringUtils.isNotBlank(shopJson)){
                return JSONUtil.toBean(shopJson,Shop.class);
            }
            if(shopJson!=null){
                return null;
            }
            //不存在从数据库查询
            Shop shop = this.getById(id);
            if(shop == null){
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //不存在返回null
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
            //存在 写入redis缓存
            //返回数据库数据
            return shop;
        }*/
      /*  private  boolean tryLock(String key){
            boolean flag =stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10, TimeUnit.SECONDS);
            return BooleanUtil.isTrue(flag);
        }
        private void unlock(String key){
            stringRedisTemplate.delete(key);

        }*/






        private void saveShopTORedis(long id, long expireSeconds){
            Shop shop = getById(id);
            RedisData redisData = new RedisData();
            redisData.setData(shop);
            redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));

        }
        @Override
        public Object update(Shop shop) {
            long id = shop.getId();
            //判断验id是否存在
            if(Objects.isNull(id)){
                return Result.fail("商铺id不能为空");

            }
            //更新数据库
            this.updateById(shop);
            //删除缓存
            stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());
            //返回成功
            return Result.ok();
        }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
            //是否根据坐标查询
        if(x==null||y==null){
            Page<Shop> page = query()
                    .eq ("type_id", typeId)
                    .page (new Page<> (current, SystemConstants. DEFAULT_PAGE_SIZE)) ;
// 返回数据
            return Result.ok (page.getRecords ()) ;
        }
            //计算分页参数
        int form = (current-1)*SystemConstants. DEFAULT_PAGE_SIZE;
        int end = current*SystemConstants. DEFAULT_PAGE_SIZE;
            //查询redis，按距离分页排序

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        "shop:geo"+typeId,
                        GeoReference.fromCoordinate(x,y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)

 );
            //解析id
        if(results==null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results. getContent();
        if (list.size () <= form) {
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<> (list.size());
        list.stream().skip(form) .skip(end) .forEach(result -> {

            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            Distance distance = result.getDistance();
            distanceMap.put (shopIdStr, distance) ;

                });
            //根据id查询数据库
            String idStr = StrUtil.join(",",ids);
            List<Shop> shops= query()
                    .in("id", ids)
                    .last("ORDER BY FIELD(id ," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        //返回数据
        return Result.ok(shops);
    }


}
