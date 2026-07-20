package com.fs.controller.user;

import com.fs.context.BaseContext;
import com.fs.result.Result;
import com.fs.task.DishRecommendTask;
import com.fs.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端每日推荐菜品接口。
 */
@RestController
@RequestMapping("/user/dishRecommend")
@Api(tags = "C端每日推荐菜品接口")
public class DishRecommendController {

    @Autowired
    private DishRecommendTask dishRecommendTask;

    /**
     * 返回当天随机推荐的三道在售菜品。
     */
    @GetMapping("/list")
    @ApiOperation("查询今日推荐菜品")
    public Result<List<DishVO>> list() {
        return Result.success(
                dishRecommendTask.getTodayRecommendDishes(BaseContext.getCurrentId())
        );
    }
}
