package com.lif314.gulimall.order.to;

import lombok.Data;

import java.util.List;

/**
 * 订单保存成功后的锁定库存
 */
@Data
public class WareSkuLockVo {
    private String orderSn; // 订单号

    private List<SkuItemLockTo> locks;
}
