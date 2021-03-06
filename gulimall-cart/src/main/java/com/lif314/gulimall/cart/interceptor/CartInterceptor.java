package com.lif314.gulimall.cart.interceptor;


import com.alibaba.fastjson.JSON;
import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.constant.CartConstant;
import com.lif314.common.to.MemberRespTo;
import com.lif314.gulimall.cart.to.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 拦截器: 在执行目标方法之前，先判断用户的登录状态，
 * 并封装传递给controller目标请求
 */
//@Component  // 拦截器是一个组件
public class CartInterceptor implements HandlerInterceptor {

    // ThreadLocal 同一线程之间共享数据 --- Map(线程号, 共享的数据)
    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * 在目标方法执行之前执行
     *
     * @param request  请求
     * @param response 响应
     * @param handler  执行方法
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 获取Session,从Session中获取当前登录用户
        UserInfoTo userInfoTo = new UserInfoTo();

        HttpSession session = request.getSession();
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        MemberRespTo member = JSON.parseObject(JSON.toJSONString(attribute), MemberRespTo.class);
        if (member != null) {
            // 登录
            userInfoTo.setUserId(member.getId());

        }

        // 没有登录,创建临时用户，查看临时购物车
        // 从cookie中获取信息
        Cookie[] cookies = request.getCookies();
        if(cookies != null && cookies.length > 0){
            for (Cookie cookie : cookies) {
                //  user-key
                String name = cookie.getName();
                if(name.equals(CartConstant.TEMP_USER_COOKIE_NAME)){
                    // 该临时用户已经存在--获取临时用户信息
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }

            }
        }
        // 如果没有临时用户，则创建一个
        if(StringUtils.isEmpty(userInfoTo.getUserKey())){
            String userKey = UUID.randomUUID().toString();
            userInfoTo.setUserKey(userKey);
        }

        // 在目标方法执行之前，使用threadLocal.
        // 这样目标方法就可以快速获取用户信息
        threadLocal.set(userInfoTo);
        // 全部放行
        return true;
    }


    /**
     * 业务执行之后，命令浏览器保存一个cookie信息，该信息一个月后失效
     *
     * 分配临时用户
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

        UserInfoTo userInfoTo = threadLocal.get();
        if(!userInfoTo.isTempUser()){
            // 如果没有临时用户信息，则放在cookie中
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME,userInfoTo.getUserKey());
            // 设置cookie的作用域
            cookie.setDomain("feihong.com");
            // 设置过期时间 -- 一个月
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            // 添加cookie
            response.addCookie(cookie);
        }
    }
}
