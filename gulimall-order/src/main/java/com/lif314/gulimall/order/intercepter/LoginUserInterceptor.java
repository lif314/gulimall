package com.lif314.gulimall.order.intercepter;


import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.to.MemberRespTo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 用户登录拦截器
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {


    // 使用ThreadLocal共享用户数据
    public static ThreadLocal<MemberRespTo> loginUser = new ThreadLocal<>();
    /**
     * 预处理
     * @param request 请求
     * @param response 响应
     * @param handler 处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HttpSession session = request.getSession();
        MemberRespTo attribute = (MemberRespTo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute != null){
            // 登录
            loginUser.set(attribute);  // 共享用户数据
            return true;
        }else{
            // 没有登录就去登录
            request.getSession().setAttribute("msg", "请先登录！");
            response.sendRedirect("http://auth.feihong.com/login.html");
            return false;
        }
    }
}
