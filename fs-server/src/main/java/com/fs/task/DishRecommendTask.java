package com.fs.task;

import com.fs.component.MapDistanceComponent;
import com.fs.constant.StatusConstant;
import com.fs.entity.AddressBook;
import com.fs.entity.Dish;
import com.fs.mapper.AddressBookMapper;
import com.fs.service.DishService;
import com.fs.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 每日推荐任务：每天 06:00 随机选择三道在售菜品。
 */
@Component
@Slf4j
public class DishRecommendTask {

    @Autowired
    private DishService dishService;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private MapDistanceComponent mapDistanceComponent;

    /**
     * 当日推荐缓存。重启后会在首次查询时重新生成。
     */
    private volatile List<DishVO> todayRecommendDishes = Collections.emptyList();

    /**
     * 每天 06:00 更新推荐。
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public synchronized void recommendDishes() {
        log.info("开始生成每日推荐菜品，时间：{}", LocalDateTime.now());

        Dish condition = new Dish();
        condition.setStatus(StatusConstant.ENABLE);
        List<DishVO> availableDishes = dishService.listWithFlavor(condition);

        if (availableDishes == null || availableDishes.isEmpty()) {
            todayRecommendDishes = Collections.emptyList();
            log.warn("当前没有起售中的菜品，今日推荐为空");
            return;
        }

        List<DishVO> shuffledDishes = new ArrayList<>(availableDishes);
        Collections.shuffle(shuffledDishes);
        todayRecommendDishes = new ArrayList<>(
                shuffledDishes.subList(0, Math.min(3, shuffledDishes.size()))
        );

        log.info("今日推荐菜品：{}", todayRecommendDishes.stream()
                .map(DishVO::getName)
                .collect(Collectors.joining("、")));
    }

    public List<DishVO> getTodayRecommendDishes() {
        if (todayRecommendDishes.isEmpty()) {
            recommendDishes();
        }
        return new ArrayList<>(todayRecommendDishes);
    }

    /**
     * 配置高德 Web 服务 Key 且用户具有收货地址时，增加 5 公里配送范围过滤。
     * 未配置地图服务或用户未填写地址时，正常降级，不阻断推荐菜品展示。
     */
    public List<DishVO> getTodayRecommendDishes(Long userId) {
        List<DishVO> dishes = getTodayRecommendDishes();
        if (dishes.isEmpty() || userId == null) {
            return dishes;
        }

        List<AddressBook> addressList = addressBookMapper.listByUserId(userId);
        if (addressList == null || addressList.isEmpty()) {
            return dishes;
        }

        AddressBook addressBook = addressList.get(0);
        String address = Stream.of(
                        addressBook.getProvinceName(),
                        addressBook.getCityName(),
                        addressBook.getDistrictName(),
                        addressBook.getDetail())
                .filter(Objects::nonNull)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining());

        if (!StringUtils.hasText(address)) {
            return dishes;
        }

        try {
            return mapDistanceComponent.isWithinRange(address)
                    ? dishes
                    : Collections.emptyList();
        } catch (Exception exception) {
            log.warn("推荐菜品距离校验失败，使用正常推荐结果：{}", exception.getMessage());
            return dishes;
        }
    }
}
