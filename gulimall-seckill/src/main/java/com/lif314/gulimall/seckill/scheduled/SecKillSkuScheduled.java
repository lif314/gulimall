package com.lif314.gulimall.seckill.scheduled;


import com.lif314.gulimall.seckill.service.SecKillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


/**
 * 秒杀商品定时上架：
 *      每天晚上三点上架最近三天内要秒杀的商品
 *      当天00:00:00 - 23:59:59
 *      明天00:00:00 - 23:59:59
 *      后天00:00:00 - 23:59:59
 */
@Service
@Slf4j
public class SecKillSkuScheduled {

    @Autowired
    SecKillService secKillService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void updateSecKillSkuLatest3Days(){
        // TODO 重复上架无需处理
        secKillService.updateSecKillSkuLatest3Days();

    }


}
