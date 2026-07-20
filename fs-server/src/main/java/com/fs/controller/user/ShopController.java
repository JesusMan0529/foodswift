package com.fs.controller.user;

import com.fs.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /***
     * 获取店铺的营业状态
     * @return
     */
    @GetMapping("/status")
    public Result getStatus(){
        try {
            Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
            log.info("获取店铺的营业状态为：{}", status == null ? "未知" : (status == 1 ? "营业中" : "打烊中"));
            return Result.success(status);
        } catch (Exception e) {
            log.warn("Redis 不可用，返回默认营业状态：{}", e.getMessage());
            return Result.success(1); // 默认营业中
        }
    }
}
