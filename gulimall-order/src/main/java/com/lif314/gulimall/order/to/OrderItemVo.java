package com.lif314.gulimall.order.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {
    private Long skuId; // 商品id

    private Boolean check=true; // 是否被选中

    private String title; // 标题

    private String image;   // 图片

    private List<String> skuAttr; // 选中的商品的属性列表

    private BigDecimal price;  // 价格

    private Integer count;   // 数量

    private BigDecimal totalPrice;  // 数量*价格，
}
