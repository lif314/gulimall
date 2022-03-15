package com.lif314.gulimall.order.web;


import com.lif314.gulimall.order.service.OrderService;
import com.lif314.gulimall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.applet.AudioClip;

@Controller
public class WebOrderController {

    @Autowired
    OrderService orderService;


    @GetMapping("/{page}.html")
    public String testPage(@PathVariable("page") String page) {
        return page;
    }


    /**
     * 处理去结算请求
     */
    @GetMapping("/toTrade")
    public String toTrade(){
        // 获取选中的商品数据
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        return "confirm";
    }


}
