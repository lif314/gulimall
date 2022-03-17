package com.lif314.gulimall.ware.vo;

import lombok.Data;

/**
 * 每件商品锁几件
 */
@Data
public class SkuItemLockTo {

    private String skuId;

    private Integer count;
}
