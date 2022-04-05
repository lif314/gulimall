package com.lif314.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lif314.common.constant.SecKillConstant;
import com.lif314.common.utils.R;
import com.lif314.gulimall.seckill.feign.CouponFeignService;
import com.lif314.gulimall.seckill.feign.ProductFeignService;
import com.lif314.gulimall.seckill.service.SecKillService;
import com.lif314.gulimall.seckill.to.SecKillSkuRedisTo;
import com.lif314.gulimall.seckill.vo.SeckillSessionVo;
import com.lif314.gulimall.seckill.vo.SeckillSkuRelationVo;
import com.lif314.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class SecKillServiceImpl implements SecKillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public void updateSecKillSkuLatest3Days() {
        // 1、扫描最近三天内需要参与的秒杀活动
        R r = couponFeignService.getLatest3DaysSession();
        if(r.getCode() == 0){
            // 获取成功
            Object data = r.get("data");
            Object o = JSONObject.toJSON(data);
            String s = JSON.toJSONString(o);
            List<SeckillSessionVo> seckillSessionVos = JSON.parseObject(s, new TypeReference<List<SeckillSessionVo>>(){});

            // 2、上架商品，将商品存储在Redis中
            //  缓存活动信息
            saveSessionInfos(seckillSessionVos);
            // 缓存活动的关联信息
            saveRelationSkusInfos(seckillSessionVos);
        }
    }

    // 缓存活动信息
    private void saveSessionInfos(List<SeckillSessionVo> seckillSessionVos){

        seckillSessionVos.forEach((sessionVo)->{
            //时间当作Key
            Long startTime = sessionVo.getStartTime().getTime();
            Long endTime = sessionVo.getEndTime().getTime();
            String key = startTime + "_" + endTime;
            List<String> collect = sessionVo.getRelationSkus().stream().map((item)-> item.getId().toString()).collect(Collectors.toList());
            redisTemplate.opsForList().leftPushAll(SecKillConstant.SESSION_CACHE_PREFIX + key, collect);
        });

    }

    // 缓存活动中的商品信息
    private void saveRelationSkusInfos(List<SeckillSessionVo> seckillSessionVos){

        seckillSessionVos.forEach((session)->{
            // 准备Hasp操作
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SecKillConstant.SKUKILL_CACHE_PREFIX);

           session.getRelationSkus().forEach((skuRelationVo)->{
              // 保存SKU详细信息
               SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
               // 1、秒杀信息
               BeanUtils.copyProperties(skuRelationVo, redisTo);
               Long skuId = skuRelationVo.getSkuId();
               // 2、远程查询商品的详细信息
               R r = productFeignService.getSkuInfo(skuRelationVo.getSkuId());
               if(r.getCode() == 0){
                   Object skuInfo = r.get("skuInfo");
                   String s = JSON.toJSONString(skuInfo);
                   SkuInfoVo skuInfoVo = JSON.parseObject(s, new TypeReference<SkuInfoVo>() {
                   });
                   redisTo.setSkuInfoVo(skuInfoVo);
               }
               // 3、当前商品的开始时间和结束时间
               redisTo.setStartTime(session.getStartTime().getTime());
               redisTo.setEndTime(session.getEndTime().getTime());
               // 4、随机码：防止攻击！！秒杀开始的时刻才生成
               String token = UUID.randomUUID().toString().replace("-", "");
               redisTo.setRandomCode(token);

               // 5、商品分布式信号量(自增量:限流)：高并发扣库存，只要进来一个请求，则信号量减一
               // 减成功的才处理数据库
               // 按照随机码进行减库存而不是按照商品ID，防止秒杀前恶意请求(已知商品ID)
               // 使用redisson
               RSemaphore semaphore = redissonClient.getSemaphore(SecKillConstant.SKU_STOCK_SEMAPHORE + token);
               // 将存库设置为信号量，秒杀请求成功就减1
               semaphore.trySetPermits(skuRelationVo.getSeckillCount());

               // 保存在Redis中
               String s = JSON.toJSONString(redisTo);
               hashOps.put(skuRelationVo.getSkuId().toString(), s);
           });

        });
    }


}
