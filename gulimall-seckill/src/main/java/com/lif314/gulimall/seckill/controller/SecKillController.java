package com.lif314.gulimall.seckill.controller;

import com.lif314.common.utils.R;
import com.lif314.gulimall.seckill.service.SecKillService;
import com.lif314.gulimall.seckill.to.SecKillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SecKillController {

    @Autowired
    SecKillService secKillService;
    /**
     * 查询当前时间可以参与的秒杀商品信息
     */
    @GetMapping("/currentSecKillSkus")
    public R getCurrentSecKillSkus(){
        List<SecKillSkuRedisTo> redisToList =  secKillService.getCurrentSecKillSkus();
        return R.ok().put("data", redisToList);
    }


    /**
     * 查询某商品的秒杀信息
     */
    @GetMapping("/seckill/seckillInfo/{skuId}")
    public R getSeckillInfoBySkuId(@PathVariable("skuId") Long skuId){
        SecKillSkuRedisTo data = secKillService.getSkuSeckillInfo(skuId);
        return R.ok().put("data", data);
    }


}
