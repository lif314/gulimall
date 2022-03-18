package com.lif314.gulimall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class CartItemPriceMapVo {
    Map<Long, BigDecimal> itemNewPrice;
}
