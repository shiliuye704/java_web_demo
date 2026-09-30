package com.sky.controller.user;


import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "菜品相关接口")
public class DishController {


    @Autowired
    private DishService dishService;

    @GetMapping("/list")
    @ApiOperation("根据id查询菜品")
    public Result<List<DishVO>> getDishesWithFlavor (@RequestParam Long categoryId) {
        log.info("查询id为{}的菜品",categoryId);
        return Result.success(dishService.listWithFlavor(categoryId));
    }

}
