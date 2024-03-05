package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetMealDishMapper setMealDishMapper;
@Autowired
private DishMapper dishMapper;


    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        //向套餐表加入一个数据
        setmealMapper.save(setmeal);
        //获取套餐id
        Long id = setmeal.getId();
        //获取套餐设置的菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //设置菜品的套餐id
        setmealDishes.forEach(setmealDish ->
                setmealDish.setSetmealId(id));
        //关联菜品与套餐
   setMealDishMapper.insertBatch(setmealDishes);
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
       Page<SetmealVO> page=setmealMapper.pageQuery(setmealPageQueryDTO);


        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        //是否起售
        ids.forEach(id ->
        {
            Setmeal setmeal = setmealMapper.findById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });
        //删除
        ids.forEach(id->{
            setmealMapper.delete(id);
            setMealDishMapper.delete(id);
        });


        }

    @Override
    public SetmealVO getById(Long id) {
Setmeal setmeal=setmealMapper.findById(id);
List<SetmealDish> setmealDishes=setMealDishMapper.geiBySetmealId(id);
SetmealVO setmealVO=new SetmealVO();
BeanUtils.copyProperties(setmeal,setmealVO);
setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        //修改套餐表 执行update
        setmealMapper.update(setmeal);

        //删除套餐和菜品的关联  执行delete
setMealDishMapper.delete(setmeal.getId());
        //重新关联 执行insert
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish:setmealDishes){
            setmealDish.setSetmealId(setmeal.getId());
        }
        setMealDishMapper.insertBatch(setmealDishes);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
     //套餐中菜品是否停售  停售则无法上线
        if (status==StatusConstant.ENABLE){
              List<Dish> list=dishMapper.getBySetmealId(id);
              if (!(list.size()==0&&list==null)){
                  list.forEach(dish -> {
                      if (dish.getStatus()==StatusConstant.DISABLE){
                          throw  new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                      }
                  });
              }
        }

        Setmeal setmeal=Setmeal.builder()
                .id(id)
                .status(status)
                .build();
setmealMapper.update(setmeal);

    }


}


