package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
class HmDianPingApplicationTests {
   @Resource
    private StringRedisTemplate stringRedisTemplate;
   @Resource
    private ShopServiceImpl shopService;
   @Test
    void loadShopData() {
       //查询店铺信息
       List<Shop> shopList = shopService.list();
       //把店铺分组
        Map<Long, List<Shop>> shopMap = shopList.stream()
                .collect(Collectors.groupingBy(Shop::getTypeId));
       //分批完整保存
       for(Map.Entry<Long,List<Shop>> entry:shopMap.entrySet()){
           Long typeId = entry.getKey();
           String key = "shop:geo:"+typeId;
           List<Shop> value = entry.getValue();
           List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
           //保存店铺信息
           for (Shop shop : value) {
               //stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
               locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),new Point(shop.getX(),shop.getY())));
           }
           stringRedisTemplate.opsForGeo().add(key,locations);
           }
   }
}
