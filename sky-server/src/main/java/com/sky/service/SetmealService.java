package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    void saveWithDish(SetmealDTO setmealDTO);

    void delete(List<Long> ids);

    SetmealVO getByIdWithDish(Long id);

    void updateWithSetmealDishes(SetmealDTO setmealDTO);

    void startOrStop(Integer status, Long id);

    List<Setmeal> listByCategoryId(Long categoryId);

    List<DishItemVO> getDishesById(Integer id);
}
