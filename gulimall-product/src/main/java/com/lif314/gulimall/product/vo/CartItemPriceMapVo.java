package com.lif314.gulimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class CartItemPriceMapVo {
    Map<Long, BigDecimal> itemNewPrice;
}
