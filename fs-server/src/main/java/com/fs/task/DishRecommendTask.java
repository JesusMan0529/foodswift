package com.fs.task;

import com.fs.constant.StatusConstant;
import com.fs.entity.Dish;
import com.fs.mapper.DishMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/***
 * 定时任务：每天 06:00 随机推荐三道菜品
 */
@Component
@Slf4j
public class DishRecommendTask {

    @Autowired
    private DishMapper dishMapper;

    /***
     * 当日推荐菜品缓存，供 controller 读取
     */
    private volatile List<Dish> todayRecommendDishes = new ArrayList<>();

    /***
     * 每天 06:00 执行，随机挑选三道起售中的菜品
     */
    @Scheduled(cron = "0 0 6 * * ?")// 每天 0600 触发
    public void recommendDishes() {
        log.info("定时推荐三道随机菜品：{}", LocalDateTime.now());

        // 查询所有起售中的菜品
        Dish dish = new Dish();
        dish.setStatus(StatusConstant.ENABLE);
        List<Dish> dishList = dishMapper.list(dish);

        if (dishList == null || dishList.size() == 0) {
            log.warn("当前没有起售中的菜品，今日推荐为空");
            todayRecommendDishes = new ArrayList<>();
            return;
        }

        // 随机打乱后取前三道
        Collections.shuffle(dishList);
        todayRecommendDishes = new ArrayList<>(dishList.subList(0, Math.min(3, dishList.size())));

        log.info("今日推荐菜品：{}", todayRecommendDishes.stream()
                .map(Dish::getName).collect(Collectors.joining("、")));
    }

    /***
     * 获取当日推荐菜品（供controller调用）
     * 若缓存为空（如服务启动后尚未到06:00），则立即生成一次推荐
     * @return
     */
    public List<Dish> getTodayRecommendDishes() {
        if (todayRecommendDishes.isEmpty()) {
            recommendDishes();
        }
        return todayRecommendDishes;
    }
}
