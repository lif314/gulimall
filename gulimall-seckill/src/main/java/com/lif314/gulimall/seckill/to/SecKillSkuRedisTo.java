package com.lif314.gulimall.seckill.to;

import com.lif314.gulimall.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class SecKillSkuRedisTo {

    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private BigDecimal seckillCount;
    /**
     * 每人限购数量
     */
    private BigDecimal seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;


    // Sku详细信息
    private SkuInfoVo skuInfoVo;

    // 秒杀开始时间
    private Long startTime;

    // 秒杀结束时间
    private Long endTime;

    /**
     * 随机码
     */
    private String randomCode;
}