package com.lif314.common.constant;

public class SecKillConstant {
    // 秒杀活动redis前缀
    public static final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    // 秒杀活动中商品信息
    public static final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    // 秒杀库存处理信号量
    public static final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; // + 随机码
}
