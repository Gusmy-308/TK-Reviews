package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Object queryAllSorted() {
        //去redis中查询整张表信息
        String typeListJson = stringRedisTemplate.opsForValue().get("shop:type-list");
        //存在则直接返回
        if (typeListJson != null) {
            List<ShopType> shopTypeList = JSONUtil.toList(typeListJson, ShopType.class);
            return Result.ok(shopTypeList);
        }
        //不存在则查询数据库
        List<ShopType> shopTypeList =  this.getBaseMapper().selectList(null);
        //数据库不存在则返回
        if (shopTypeList.isEmpty()) {
            return Result.fail("数据库中没有数据");
        }
        //存在缓存到redis中
        stringRedisTemplate.opsForValue().set("shop:type-list", JSONUtil.toJsonStr(shopTypeList));
        //返回数据库中的数据
        return Result.ok(shopTypeList);
    }
}
