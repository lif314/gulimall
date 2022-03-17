package com.lif314.gulimall.order.vo;

import com.lif314.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * 下单相应数据
 */
@Data
public class SubmitOrderRespVo {

    private OrderEntity order;

    private Integer code;  // 状态码
}
