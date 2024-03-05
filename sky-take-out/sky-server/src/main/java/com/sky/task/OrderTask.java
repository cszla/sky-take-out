package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
@Scheduled(cron = "0 * * * * ?")
    public void processTimeOut(){
    log.info("支付定时任务");
        //订单超时15min
            LocalDateTime time=LocalDateTime.now().plusSeconds(-15);
         //查看下单时间  设置其状态
       List<Orders> list= orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT,time);
       if (list!=null&&list.size()>0){
           for (Orders orders : list) {
               orders.setCancelTime(LocalDateTime.now());
               orders.setCancelReason("超时支付");
               orders.setStatus(Orders.CANCELLED);
               orderMapper.update(orders);
           }
       }

    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processDiliveryOrder(){
        log.info("配送定时任务");
        //订单超时15min
        LocalDateTime time=LocalDateTime.now().plusMinutes(-60);
        //查看下单时间  设置其状态
        List<Orders> list= orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS,time);
        if (list!=null&&list.size()>0){
            for (Orders orders : list) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }

    }
}
