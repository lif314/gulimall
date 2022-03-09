package com.lif314.gulimall.authserver.controller;

import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.exception.BizCodeEnum;
import com.lif314.common.to.SmsTo;
import com.lif314.common.utils.R;
import com.lif314.gulimall.authserver.feign.ThirdPartySerrvice;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
public class LoginController {

    /**
     * 调用第三方服务发送验证码
     */
    @Autowired
    ThirdPartySerrvice thirdPartySerrvice;

    @Autowired
    StringRedisTemplate redisTemplate;

    @ResponseBody // 返回JSON数据
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){
        // TODO 验证码接口防刷
        // TODO 验证码再次校验  Redis  key-phone value-code

        // 先从Redis中查找当前手机的验证码是否已经存在，以及是否超过60s
        String nowRedisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(StringUtils.isNotEmpty(nowRedisCode)){
            long time = Long.parseLong(nowRedisCode.split("_")[1]);
            if(System.currentTimeMillis() - time < 60000){
                // 验证码发送时间在60s以内，不能继续发送
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(),BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        // 生成6位随机数验证码
        String SYMBOLS = "0123456789"; // 数字
        Random RANDOM = new SecureRandom();
        char[] nonceChars = new char[6];
        for (int index = 0; index < nonceChars.length; ++index) {
            nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
        }
        String code =  new String(nonceChars);

//        String code = UUID.randomUUID().toString().substring(0,5);
        // 防刷：在redis中存入当前时间，下一次发送请求时，查看是否在60s内
        String redisCode = code + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, redisCode, 10, TimeUnit.MINUTES);

        // 调用第三方服务发送验证码
        SmsTo smsTo = new SmsTo();
        smsTo.setPhone(phone);
        smsTo.setCode(code);
        thirdPartySerrvice.sendCode(smsTo);
        return R.ok();
    }
}
