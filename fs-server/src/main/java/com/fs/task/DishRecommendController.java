package com.fs.task;

import com.fs.context.BaseContext;
import com.fs.entity.Dish;
import com.fs.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 每日推荐菜品接口
 */
@RestController
@RequestMapping("/user/dishRecommend")
@Api(tags = "C端-每日推荐菜品接口")
public class DishRecommendController {

    @Autowired
    private DishRecommendTask dishRecommendTask;

    /**
     * 查询今日推荐的三道随机菜品
     * 仅当店铺在当前登录用户收货地址的配送范围内时才会返回推荐
     *
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询今日推荐的三道随机菜品")
    public Result<List<Dish>> list() {
        List<Dish> list = dishRecommendTask.getTodayRecommendDishes(BaseContext.getCurrentId());
        return Result.success(list);
    }
}
