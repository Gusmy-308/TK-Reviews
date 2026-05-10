package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        if (isFollow){
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if(isSuccess) {
                stringRedisTemplate.opsForSet().add("follow:" + userId, followUserId.toString());
            }
        }else{
           boolean isSuccess = remove(new QueryWrapper<Follow>()
                   .eq("user_id",userId)
                   .eq("follow_user_id",followUserId));
            if(isSuccess){
                stringRedisTemplate.opsForSet().remove("follows:"+userId,followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        Follow follow = getOne(new QueryWrapper<Follow>()
                .eq("user_id",userId)
                .eq("follow_user_id",followUserId));
        if(follow==null){
            return Result.fail("未关注");
        }
        return Result.ok("已关注");
    }

    @Override
    public Result commonFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        String key1= "follows:"+followUserId;
        String key2= "follows:"+userId;
        // 查询共同好友
        Set<String> followIds = stringRedisTemplate.opsForSet().members(key1);
        Set<String> myFollowIds = stringRedisTemplate.opsForSet().members(key2);
        // 取交集
        Set<String> commonFollowIds = stringRedisTemplate.opsForSet().intersect(key1,key2);
        if(commonFollowIds.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = commonFollowIds.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> userDtos = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDtos);
    }
}
