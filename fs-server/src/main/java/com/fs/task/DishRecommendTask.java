package com.fs.task;

import com.fs.component.MapDistanceComponent;
import com.fs.constant.StatusConstant;
import com.fs.entity.AddressBook;
import com.fs.entity.Dish;
import com.fs.mapper.AddressBookMapper;
import com.fs.mapper.DishMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * 定时任务：每天 06:00 随机推荐三道菜品
 */
@Component
@Slf4j
public class DishRecommendTask {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private MapDistanceComponent mapDistanceComponent;

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
     * 获取当日推荐菜品
     * 若缓存为空（如服务启动后尚未到06:00），则立即生成一次推荐
     * @return
     */
    public List<Dish> getTodayRecommendDishes() {
        if (todayRecommendDishes.isEmpty()) {
            recommendDishes();
        }
        return todayRecommendDishes;
    }

    /***
     * 获取指定用户的当日推荐菜品
     * 仅当店铺在该用户收货地址的配送范围内时，菜品才能进入推荐
     * @param userId
     * @return
     */
    public List<Dish> getTodayRecommendDishes(Long userId) {
        List<Dish> dishes = getTodayRecommendDishes();
        if (dishes.isEmpty()) {
            return dishes;
        }

        // 查询用户收货地址（优先默认地址，其次任意一条地址）
        AddressBook query = new AddressBook();
        query.setUserId(userId);
        query.setIsDefault(1);
        List<AddressBook> addressList = addressBookMapper.list(query);
        if (addressList == null || addressList.isEmpty()) {
            query.setIsDefault(null);
            addressList = addressBookMapper.list(query);
        }
        if (addressList == null || addressList.isEmpty()) {
            //用户未填写收货地址，无法计算距离，不做距离过滤
            log.info("用户{}未设置收货地址，今日推荐不做距离过滤", userId);
            return dishes;
        }

        // 拼接完整收货地址（与下单流程保持一致）
        AddressBook addressBook = addressList.get(0);
        String address = Stream.of(
                        addressBook.getProvinceName(),
                        addressBook.getCityName(),
                        addressBook.getDistrictName(),
                        addressBook.getDetail())
                .filter(Objects::nonNull)
                .collect(Collectors.joining());

        // 校验店铺是否在该地址的配送范围内
        boolean withinRange;
        try {
            withinRange = mapDistanceComponent.isWithinRange(address);
        } catch (Exception e) {
            //地图服务异常时降级为正常推荐，避免影响接口可用性
            log.error("推荐菜品距离校验失败，降级为正常推荐：{}", e.getMessage());
            return dishes;
        }

        if (!withinRange) {
            //超出配送范围，商家菜品不进入推荐
            log.info("用户{}收货地址超出店铺配送范围，今日不推荐菜品", userId);
            return new ArrayList<>();
        }
        return dishes;
    }
}
