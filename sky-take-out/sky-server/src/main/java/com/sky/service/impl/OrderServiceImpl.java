package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;
@Autowired
private ShoppingCartMapper shoppingCartMapper;
@Autowired
private AddressBookMapper addressBookMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
//处理业务异常 购物车为空，地址为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart=new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
if (list==null||list.size()==0){
throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
}
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
   if (addressBook==null ){
       throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
   }
  // CheckOutOfRange(addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail());

   //向订单表插入一条数据
        Orders orders=new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);
        //明细条插入n条数据
List<OrderDetail> orderDetailList=new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail=new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
  orderDetailMapper.insert(orderDetailList);

//清空购物车
        shoppingCartMapper.delete(userId);
        //封装vo返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
       // Long userId = BaseContext.getCurrentId();
        //User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
       /* JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );
*/
        OrderPaymentVO vo=null;
        Long userId = BaseContext.getCurrentId();
        List<Orders> list = orderMapper.getByUserId(userId);
        for (Orders orders : list) {
            if (orders.getStatus()==Orders.PENDING_PAYMENT) {
                JSONObject jsonObject = new JSONObject();
                if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
                    throw new OrderBusinessException("该订单已支付");
                }
                 vo = jsonObject.toJavaObject(OrderPaymentVO.class);
                vo.setPackageStr(jsonObject.getString("package"));
            }
        }
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */

    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        log.info("成功支付");
        Map map=new HashMap();
        map.put("type",1);
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号："+outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);

    }

    @Override
    public PageResult pageQuery(int page, int pageSize, Integer status) {
        //分页
        PageHelper.startPage(page, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO=new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
       //分页条件查询
       Page<Orders> pages =orderMapper.pageQuery(ordersPageQueryDTO);
List<OrderVO> list=new ArrayList<>();
        //查询明细 装入vo
if (pages!=null&&pages.size()>0){
    for (Orders orders : pages) {
        Long orderId = orders.getId();
      List<OrderDetail> orderDetail= orderDetailMapper.getById(orderId);
        OrderVO orderVO=new OrderVO();
         BeanUtils.copyProperties(orders,orderVO);
         orderVO.setOrderDetailList(orderDetail);
         list.add(orderVO);
    }

}

        return new PageResult(pages.getTotal(),list);
    }

    @Override
    public void cancel(Long id) {
        //查询订单是否存在
   Orders orders=orderMapper.getById(id);
        //不存在抛异常
        if (orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
if (orders.getStatus()>2){
    throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
}
// 订单处于待接单状态下取消，需要进行退款
if (orders.getStatus()==2){
    orders.setPayStatus(Orders.REFUND);
}
        // 更新订单状态、取消原因、取消时间
orders.setStatus(Orders.CANCELLED);
orders.setCancelReason("用户取消");
orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }

    @Override
    public OrderVO getOrderDetailById(Long id) {
  //是否有订单
        Orders order = orderMapper.getById(id);
        if (order==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> list = orderDetailMapper.getById(order.getId());
       OrderVO orderVO=new OrderVO();
       BeanUtils.copyProperties(order,orderVO);
       orderVO.setOrderDetailList(list);
        return orderVO;
    }

    @Override
    public void repetition(Long id) {
//获取订单
        Long userId = BaseContext.getCurrentId();

        //获取明细
        List<OrderDetail> list = orderDetailMapper.getById(id);

        //将商品重新放入购物车
        List<ShoppingCart> shoppingCartList = list.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

      shoppingCartMapper.insertBatch(shoppingCartList);

    }


   @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //查询订单
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
// 返回vo响应结果
        //获取orderDetailList
        List<OrderVO> VOlist=new ArrayList<>();
        List<Orders> orderlist = page.getResult();
        if (!CollectionUtils.isEmpty(orderlist)) {
            for (Orders orders : orderlist) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                //获取orderdishes
              String orderDishes=getOrderDishes(orders);
                orderVO.setOrderDishes(orderDishes);
                VOlist.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(),VOlist);
    }
    @Override
    public OrderStatisticsVO statistics() {

        Integer toBeConfirmed = orderMapper.statistics(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.statistics(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.statistics(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO=new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
//确定订单存在且代接单
Orders ordersDB=orderMapper.getById(ordersRejectionDTO.getId());
if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)||ordersDB==null){
    throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
}
Orders orders=new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);


    }

    @Override
    public void AdminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    @Override
    public void reminder(Long id) {
        Orders orderDB = orderMapper.getById(id);
        if (orderDB==null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Map map=new HashMap();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","订单号："+orderDB.getNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    private String getOrderDishes(Orders orders) {
        List<OrderDetail> odList = orderDetailMapper.getById(orders.getId());
        List<String> collect = odList.stream().map(x -> {
            String orderDishes = x.getName() + "*" + x.getNumber()+";";
            return orderDishes;
        }).collect(Collectors.toList());

        return String.join("",collect);
    }

    private void CheckOutOfRange(String address){
      //  检查客户的收货地址是否超出配送范围

//获取店铺的经纬度坐标
        Map<String, String> map=new HashMap<>();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);
        String shopCordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        JSONObject jsonObject = JSON.parseObject(shopCordinate);
        if (!jsonObject.getString("status").equals(0)){
            throw new OrderBusinessException("店铺地址解析失败");
        }
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        String shopLngLat=lat+lng;

        //获取用户收货地址的经纬度坐标
        map.put("address",address);
        String userCordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        jsonObject = JSON.parseObject(userCordinate);
        if (!jsonObject.getString("status").equals(0)){
            throw new OrderBusinessException("收货地址解析失败");
        }
        location=jsonObject.getJSONObject("result").getJSONObject("location");
         lat = location.getString("lat");
         lng = location.getString("lng");
        String userLngLat=lat+lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");
//路线规划

       String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);
        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }

    }
}
