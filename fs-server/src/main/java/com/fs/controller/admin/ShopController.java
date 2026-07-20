package com.fs.controller.admin;

import com.fs.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /***
     * 设置店铺营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺的营业状态为：{}", status == 1 ? "营业中" : "打烊中");
        try {
            redisTemplate.opsForValue().set(KEY, status);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过状态写入：{}", e.getMessage());
        }
        return Result.success();
    }

    /***
     * 获取店铺的营业状态
     * @return
     */
    @GetMapping("/status")
    public Result getStatus(){
        try {
            Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
            // Redis 中首次没有店铺状态时，按营业中初始化，避免 null 自动拆箱导致 500。
            if (status == null) {
                status = 1;
                redisTemplate.opsForValue().set(KEY, status);
            }
            log.info("获取店铺的营业状态为：{}", status == 1 ? "营业中" : "打烊中");
            return Result.success(status);
        } catch (Exception e) {
            log.warn("Redis 不可用，返回默认营业状态：{}", e.getMessage());
            return Result.success(1); // 默认营业中
        }
    }
}
