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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImp implements OrderService {

    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Override
    @Transactional
    public OrderSubmitVO submitOrders(OrdersSubmitDTO ordersSubmitDTO) {
        //判断地址簿是否存在
        AddressBook addressBook = addressBookMapper.getById(Math.toIntExact(ordersSubmitDTO.getAddressBookId()));
        if(addressBook==null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //判断是否超出配送范围
        checkOutOfRange(addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail());

        //判断购物车是否真的存在
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构建插入orders表的数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(BaseContext.getCurrentId());
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        //orders.setUserName();
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDistrictName()+addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());

        //插入数据
        orderMapper.insert(orders);

        //向orders_detail插入数条数据
        List<OrderDetail> list = new ArrayList<>();
        for (ShoppingCart cart:shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            list.add(orderDetail);
        }

        orderDetailMapper.insertBatch(list);

        //清空购物车
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());

        //构建返回的vo
        OrderSubmitVO vo = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();

        return vo;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal("0.01"), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
//
//        return vo;

        paySuccess(ordersPaymentDTO.getOrderNumber());
        return new OrderPaymentVO();
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
    }

    @Override
    public PageResult historyOrders(Integer page, Integer pageSize, Integer status) {
        PageHelper.startPage(page,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = OrdersPageQueryDTO.builder()
                .userId(BaseContext.getCurrentId())
                .status(status)
                .build();
        Page<Orders> ordersPage = orderMapper.getWithConditions(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();

        for (Orders orders : ordersPage) {
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders,orderVO);
            orderVO.setOrderDetailList(orderDetails);
            orderVOList.add(orderVO);
        }
        return new PageResult(ordersPage.getTotal(),orderVOList);
    }

    @Override
    public OrderVO getOrderWithDetail(Integer id) {
        Orders order=orderMapper.getById(id);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(order.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order,orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    @Override
    public void cancelById(Integer id) {
        Orders orders = orderMapper.getById(id);

        if(orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders newOrders = new Orders();
        newOrders.setId(orders.getId());
        newOrders.setStatus(Orders.CANCELLED);

        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {

            //测试使用
            newOrders.setPayStatus(Orders.REFUND);

            //实际场景使用
//            try {
//                weChatPayUtil.refund(
//                        orders.getNumber(), //商户订单号
//                        orders.getNumber(), //商户退款单号
//                        new BigDecimal("0.01"),//退款金额，单位 元
//                        new BigDecimal("0.01"));//原订单金额
//                newOrders.setPayStatus(Orders.REFUND);
//            } catch (Exception e) {
//                throw new OrderBusinessException(MessageConstant.UNKNOWN_ERROR);
//            }
        }

        newOrders.setCancelReason("用户取消");
        newOrders.setCancelTime(LocalDateTime.now());

        orderMapper.update(newOrders);
    }

    @Override
    public void repetition(Integer id) {
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(Long.valueOf(id));

        List<ShoppingCart> list = orderDetails.stream().map(orderDetail -> {
            ShoppingCart cart = new ShoppingCart();

            BeanUtils.copyProperties(orderDetail, cart, "id");
            cart.setUserId(BaseContext.getCurrentId());
            cart.setCreateTime(LocalDateTime.now());

            return cart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(list);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.getWithConditions(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();

        //判断是否查到了信息
        if(orders!=null && !orders.isEmpty()){
            for(Orders order : orders) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order,orderVO);

                //拿到订单的详细菜品信息
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(order.getId());

                //将所有菜品转成字符串
                List<String> dishes = orderDetails.stream().map(orderDetail -> {
                    String dish = orderDetail.getName() + "*" + orderDetail.getNumber() + ";";
                    return dish;
                }).collect(Collectors.toList());

                //拼接成字符串
                String dishesString = String.join("", dishes);

                //将菜品加入VO
                orderVO.setOrderDishes(dishesString);

                orderVOList.add(orderVO);
            }
        }

        return new PageResult(orders.getTotal(),orderVOList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(orderMapper.countByStatus(Orders.TO_BE_CONFIRMED));
        orderStatisticsVO.setConfirmed(orderMapper.countByStatus(Orders.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS));

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
        Orders order = orderMapper.getById(Math.toIntExact(ordersRejectionDTO.getId()));

        if (order==null || !order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();

        if(Orders.PAID.equals(order.getPayStatus())) {
            try {

                //实际支付时使用
//              weChatPayUtil.refund(
//                        order.getNumber(),
//                        order.getNumber(),
//                        new BigDecimal("0.01"),
//                        new BigDecimal("0.01"));

                orders.setPayStatus(Orders.REFUND);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        orderMapper.update(orders);
    }

    @Override
    public void delivery(Integer id) {
        Orders orders = orderMapper.getById(id);

        if(orders==null || !orders.getStatus().equals(Orders.CONFIRMED) ) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order = Orders.builder()
                .id(Long.valueOf(id))
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        orderMapper.update(order);
    }

    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.getById(Math.toIntExact(ordersCancelDTO.getId()));

        if(orders==null || orders.getStatus().equals(Orders.COMPLETED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        if(orders.getPayStatus().equals(Orders.PAID)) {
            try {

                //实际支付使用
//                weChatPayUtil.refund(
//                        orders.getNumber(),
//                        orders.getNumber(),
//                        new BigDecimal("0.01"),
//                        new BigDecimal("0.01"));

                order.setPayStatus(Orders.REFUND);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        orderMapper.update(order);
    }

    @Override
    public void complete(Integer id) {
        Orders orders = orderMapper.getById(id);

        if (orders==null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order = Orders.builder()
                .id(Long.valueOf(id))
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();

        orderMapper.update(order);
    }


    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        //获取店铺的经纬度
        String shopLngLat = getLngLat(shopAddress);

        //获取收获地址的经纬度
        String userLngLat = getLngLat(address);

        //获取商家和收货地址的距离
        Map<String,String> map = new HashMap<>();
        map.put("ak",ak);
        //注意：驾车路线规划接口要求坐标格式为“纬度,经度”(lat,lng)，与地理编码返回的“经度,纬度”(lng,lat)相反
        map.put("origin",toLatLng(shopLngLat));
        map.put("destination",toLatLng(userLngLat));
        map.put("steps_info","0");

        //向接口发送请求
        String resultString = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);
        JSONObject jsonObject = JSON.parseObject(resultString);

        //判断请求是否成功
        if(!jsonObject.getString("status").equals("0")) {
            log.error("调用百度驾车路线规划接口失败，status={}, message={}", jsonObject.getString("status"), jsonObject.getString("message"));
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //解析返回结果
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray routes = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) routes.get(0)).get("distance");

        if (distance>5000) {
            throw new OrderBusinessException("超出配送范围");
        }
    }


    //获取地址的经纬度
    private String getLngLat(String address) {

        //构造请求体
        Map<String, String> map = new HashMap<>();
        map.put("address",address);
        map.put("ak",ak);
        map.put("output","json");

        //向百度提供的接口发送请求
        String adressString = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        //将返回值转换为JSON类型
        JSONObject jsonObject = JSON.parseObject(adressString);

        //判断请求是否成功
        if(!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //解析数据
        JSONObject shopLocation = jsonObject.getJSONObject("result").getJSONObject("location");
        String lng = shopLocation.getString("lng");
        String lat = shopLocation.getString("lat");

        String LngLat = lng + "," + lat;

        return LngLat;
    }

    //将“经度,纬度”转换为百度驾车路线规划接口要求的“纬度,经度”
    private String toLatLng(String lngLat) {
        String[] parts = lngLat.split(",");
        return parts[1] + "," + parts[0];
    }
}
