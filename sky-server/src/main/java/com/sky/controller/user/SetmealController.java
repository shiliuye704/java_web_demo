package com.sky.controller.user;


import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("UserSetmealController")
@RequestMapping("/user/setmeal")
@Slf4j
@Api(tags = "套餐相关接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    @Cacheable(cacheNames = "categorySetmealCache",key = "#categoryId")
    public Result<List<Setmeal>> listByCategoryId (@RequestParam Long categoryId) {
        log.info("查询分类id为{}套餐",categoryId);
        return Result.success(setmealService.listByCategoryId(categoryId));
    }
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品")
    public Result<List<DishItemVO>> getDishesById(@PathVariable Integer id) {
        log.info("查询套餐id{}查询包含的菜品",id);
        return Result.success(setmealService.getDishesById(id));
    }
}
