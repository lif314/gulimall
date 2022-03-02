package com.lif314.common.to;

import lombok.Data;


// 远程查询是否有库存
@Data
public class SkuHasStockTo {

    private Long skuId;
    private Boolean hasStock;

}
