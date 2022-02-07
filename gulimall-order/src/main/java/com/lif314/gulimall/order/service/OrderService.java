package com.lif314.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.order.entity.OrderEntity;

import java.util.Map;

/**
 * 订单
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:03:07
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);
}
