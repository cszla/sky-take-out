package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    void insert(Orders orders);
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String outTradeNo);

    void update(Orders orders);

    @Update("update orders set status = #{orderStatus},pay_status = #{orderPaidStatus} ,checkout_time = #{check_out_time} where id = #{id}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime check_out_time, Long id);


    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);
@Select("select * from orders where id=#{id}")
    Orders getById(Long id);
@Select("select count(id) from orders where status = #{status}")
    Integer statistics(Integer status);
@Select("select * from orders where status=#{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTime(Integer status, LocalDateTime time);

@Select("select * from orders where user_id=#{userId}")
    List<Orders> getByUserId(Long userId);


    Double sumByMap(Map map);
}
