package com.lif314.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.vo.OrderConfirmVo;
import com.lif314.gulimall.order.vo.OrderSubmitVo;
import com.lif314.gulimall.order.vo.SubmitOrderRespVo;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:03:07
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderRespVo submitOrder(OrderSubmitVo submitVo);

    OrderEntity getOrderStatusByOrderSn(String orderSn);

    void closeOrder(OrderEntity entity);
}

