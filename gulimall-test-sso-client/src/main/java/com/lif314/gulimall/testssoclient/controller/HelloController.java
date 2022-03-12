package com.lif314.gulimall.testssoclient.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;

@Controller
public class HelloController {

    @Value("${sso.server.url}")
    String ssoServerUrl;
    /**
     * 无需登录就可以访问
     */
    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }


    @Autowired
    StringRedisTemplate redisTemplate;
    /**
     * 需要登录才能访问
     *
     * 需要感知是否是登录成功到达该请求
     */
    @GetMapping("/employees")
    public String employees(Model model, HttpSession session, @RequestParam(value = "token", required = false) String token){
        if(!StringUtils.isEmpty(token)){
            // token不为空，说明已经登录
            // 登录成功，获取数据进行显示
            String username = redisTemplate.opsForValue().get(token);
            // 将用户信息放在session中，session不能解决不同域名之间的问题
            session.setAttribute("loginUser", username);
            ArrayList<String> employs = new ArrayList<>();
            employs.add("欢迎你：" + username);
            employs.add("张三");
            employs.add("莉莉丝");
            model.addAttribute("employs",employs);
            return "list";
        }else{
            // 没有登录，跳到登录服务器
            // 登录成功后，服务器根据redirect_url回到原页面
            return "redirect:" + ssoServerUrl + "?redirect_url=http://client1.com:8081/employees";
        }
    }
}
