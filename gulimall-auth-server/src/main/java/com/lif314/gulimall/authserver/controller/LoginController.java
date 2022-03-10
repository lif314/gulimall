package com.lif314.gulimall.authserver.controller;

import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.exception.BizCodeEnum;
import com.lif314.common.to.SmsTo;
import com.lif314.common.utils.R;
import com.lif314.gulimall.authserver.feign.ThirdPartySerrvice;
import com.lif314.gulimall.authserver.vo.UserRegisterVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    /**
     * TODO  重定向携带数据，利用session原理，将数据放在session中
     * RedirectAttributes:模拟重定向发送数
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo vo, BindingResult bindingResult, RedirectAttributes redirectAttributes){
        // 1. 进行数据校验
        if(bindingResult.hasErrors()){
            Map<String, String> errors = bindingResult.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
//            model.addAttribute("errors", errors);
            // 重定向携带数据
            redirectAttributes.addFlashAttribute("errors", errors);
            // 如果注册失败，重新注册页
            // 防止表单重复提交 -- 转发
//            return "register"; // 会进行拼串
            // 使用重定向视图 -- 必须使用完整域名
            return "redirect:http://auth.feihong.com/register.html";  // 转发不进行拼串
        }
        // POST not supported
        // 用户注册-->/register.html[post]--> 转发/register.html 路径映射只能使用get方式访问

        // 调用远程服务进行注册


        // 2. 用户名和手机号不能是已经存在的

        // 重定向：注册成功后回到首页/回到登录页
        // registry.addViewController("/login").setViewName("login");
        // return "redirect:http://auth.feihong.com/login";
        return "redirect:/login.html";
    }

}
