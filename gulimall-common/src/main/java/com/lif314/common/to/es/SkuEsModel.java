package com.lif314.common.to.es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuEsModel {

    private Long skuId;
    private Long spuId;
    private String skuTitle;
    private BigDecimal skuPrice;
    private String skuImg;
    private Long saleCount;
    private Boolean hasStock; // 销量：只存是否有
    private Long hotScore;  // 热度评分

    private Long brandId;
    private Long catalogId;
    private String brandName;  // 冗余存储字段
    private String brandImg;
    private String catalogName;
    private List<Attrs> attrs;

    @Data // 静态内部类
    public static class Attrs {
        private Long attrId;
        private String attrName;
        private String attrValue;
    }

}
