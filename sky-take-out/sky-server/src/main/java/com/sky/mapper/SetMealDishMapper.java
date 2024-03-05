package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetMealDishMapper {


   

    public List<Long> getSetMealIdsByDishIds(List<Long> dishIds);

    void insertBatch(List<SetmealDish> setmealDishes);



@Delete("delete from setmeal_dish where setmeal_id = #{id}")
    void delete(Long id);
@Select("select * from setmeal_dish where setmeal_id=#{id}")
    List<SetmealDish> geiBySetmealId(Long id);
}
