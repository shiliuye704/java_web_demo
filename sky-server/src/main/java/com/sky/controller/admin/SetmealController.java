package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐管理")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询条件为：{}",setmealPageQueryDTO);
        return Result.success(setmealService.page(setmealPageQueryDTO));
    }

    @PostMapping
    @ApiOperation("套餐新增")
    @CacheEvict(cacheNames = "categorySetmealCache",key = "#setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐内容为：{}",setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    @DeleteMapping
    @ApiOperation("删除套餐")
    @CacheEvict(cacheNames = "categorySetmealCache",allEntries = true)
    public Result delete(@RequestParam List<Long> ids) {
        log.info("删除id为{}的套餐",ids);
        setmealService.delete(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getByIdWithDish(@PathVariable Long id) {
        log.info("查询id为{}的套餐信息",id);
        return Result.success(setmealService.getByIdWithDish(id));
    }

    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "categorySetmealCache",allEntries = true)
    public Result updateWithSetmealDishes(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐:{}",setmealDTO);
        setmealService.updateWithSetmealDishes(setmealDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("套餐起售停售")
    @CacheEvict(cacheNames = "categorySetmealCache",allEntries = true)
    public Result startOrStop(@PathVariable Integer status,@RequestParam(defaultValue = "id") Long id) {
        log.info("对id为{}的套餐更改状态为{}",id,status);
        setmealService.startOrStop(status,id);
        return Result.success();
    }
}
