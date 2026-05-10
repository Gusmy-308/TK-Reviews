package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
   @Resource
   private IFollowService followService;
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable Long followUserId, @PathVariable Boolean isFollow){
        return followService.follow(followUserId,isFollow);
    }
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable Long followUserId,@PathVariable Boolean isFollow){
       return followService.isFollow(followUserId ,isFollow);
    }
    @GetMapping("/common/{id}")
    public Result commonFollow(@PathVariable Long followUserId){
        return followService.commonFollow(followUserId);
    }


}
