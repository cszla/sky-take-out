package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        //判断购物车是否存在


            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
            //只能查询自己的购物车数据
            shoppingCart.setUserId(BaseContext.getCurrentId());

            //判断当前商品是否在购物车中
            List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

            if (shoppingCartList != null && shoppingCartList.size() == 1) {
                //如果已经存在，就更新数量，数量加1
                shoppingCart = shoppingCartList.get(0);
                shoppingCart.setNumber(shoppingCart.getNumber() + 1);
                shoppingCartMapper.update(shoppingCart);
            } else {
                //如果不存在，插入数据，数量就是1

                //判断当前添加到购物车的是菜品还是套餐
                Long dishId = shoppingCartDTO.getDishId();
                if (dishId != null) {
                    //添加到购物车的是菜品
                    Dish dish = dishMapper.getById(dishId);
                    shoppingCart.setName(dish.getName());
                    shoppingCart.setImage(dish.getImage());
                    shoppingCart.setAmount(dish.getPrice());
                } else {
                    //添加到购物车的是套餐
                    Setmeal setmeal = setmealMapper.findById(shoppingCartDTO.getSetmealId());
                    shoppingCart.setName(setmeal.getName());
                    shoppingCart.setImage(setmeal.getImage());
                    shoppingCart.setAmount(setmeal.getPrice());
                }
                shoppingCart.setNumber(1);
                shoppingCart.setCreateTime(LocalDateTime.now());
                shoppingCartMapper.insert(shoppingCart);
            }


        }

    @Override
    public List<ShoppingCart> show() {
        Long id = BaseContext.getCurrentId();
        ShoppingCart shoppingCart=ShoppingCart.builder()
                .userId(id).
                build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        return list;
    }

    @Override
    public void clean() {
        Long id = BaseContext.getCurrentId();
        shoppingCartMapper.clean(id);
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
//查询当前登录用户的购物车
        Long id = BaseContext.getCurrentId();
        ShoppingCart shoppingCart=new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setUserId(id);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list!=null&&list.size()>0){
            ShoppingCart shoppingCart1 = list.get(0);
            //数量为1 删除
if (shoppingCart1.getNumber()==1){
    shoppingCartMapper.delete(shoppingCart1.getUserId());
}else {
    shoppingCart1.setNumber(shoppingCart1.getNumber()-1);

        //不为1  减少
    shoppingCartMapper.update(shoppingCart1);
}
        }
    }
}



