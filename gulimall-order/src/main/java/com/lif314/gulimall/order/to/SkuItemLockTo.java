package com.lif314.gulimall.order.to;

import lombok.Data;

/**
 * 每件商品锁几件
 */
@Data
public class SkuItemLockTo {

    private Long skuId;

    private Integer count;
}
