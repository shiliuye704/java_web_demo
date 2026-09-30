package com.sky.mapper;

import com.sky.entity.SetmealDish;
import com.sky.vo.DishItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {


    List<Long> getSetmealIdsByIds(List<Long> ids);


    void insertBatch(List<SetmealDish> setmealDishes);

    void deleteBatch(List<Long> setmealId);

    @Select("select * from setmeal_dish where setmeal_id=#{id}")
    List<SetmealDish> getSetmealDishesByIds(Long id);



    List<DishItemVO> getDishItemsById(Integer id);

}
