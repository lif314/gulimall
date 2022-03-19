package com.lif314.common.to.mq;

import lombok.Data;

@Data
public class StockLockedTo {

    private Long id;  // 库存工作单的id

    // 添加一个订单号
    private String orderSn;

    // 每个仓库锁了某商品几件
    // 工作单详情的id
    private StockDetailTo detailTo;

}
