package com.lif314.gulimall.testssoserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;


@Controller
public class LoginController {


    @Autowired
    StringRedisTemplate redisTemplate;


    @ResponseBody
    @GetMapping("/userInfo")
    public String userInfo(@RequestParam("token") String token)
    {
        // 查询用户的信息
        String s = redisTemplate.opsForValue().get(token);
        return s;
    }

    /**
     * 返回登录页
     */
    @GetMapping("/login.html")
    public String loginPage(@RequestParam("redirect_url") String url,
                            Model model,
                            @CookieValue(value = "sso_token", required = false) String sso_token){
        // 使用cookie
        if(!StringUtils.isEmpty(sso_token)){
            // 已经登录过,返回到你之前的地址
            return "redirect:" + url  + "?token=" + sso_token;
        }
        model.addAttribute("url", url);
        return "login";
    }

    /**
     *  处理登录请求
     * @param url 登录后的返回url
     */
    @PostMapping("/doLogin")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        @RequestParam("url") String url,
                        HttpServletResponse response){
        if(!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)){
            // 登录成功后，跳回到之前的页面
            // 把登录成功的用户保存起来--这里使用Redis
            String uuid = UUID.randomUUID().toString().replace("-", "");
            Cookie cookie = new Cookie("sso_token", uuid);
            // 命令浏览器保存cookie,浏览器访问这个域名都要带着cookie
            response.addCookie(cookie);  // cookie与域名绑定，即在域名内共享
            redisTemplate.opsForValue().set(uuid, username);
            // 为了让客户端感知到是登录后才返回的，可以带一个token参数
            return "redirect:" + url + "?token=" + uuid;
        }
        // 登录失败
        return "login";
    }
}
