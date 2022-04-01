package com.lif314.gulimall.order.web;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.order.config.AlipayTemplate;
import com.lif314.gulimall.order.service.OrderService;
import com.lif314.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class WebPayController {


    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    /**
     * 支付订单
     * 1、将支付页面直接交给浏览器进行渲染
     * 2、支付成功后跳转到用户页面
     * th:href= "'http://order.feihong.com/PayOrder?orderSn='+${submitOrderResp.order.orderSn}"
     */
    @ResponseBody
    @GetMapping(value = "/payOrder", produces = "text/html")  // 告诉产生html数据，而不是json数据
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        // 获取订单的支付信息
        PayVo payVo = orderService.getOrderByOrderSn(orderSn);
        // 调用alipay -- 响应数据为HTML表单
        // 将此页面直接交给浏览器进行渲染
        String pay = alipayTemplate.pay(payVo);
        return pay;
    }


    /**
     * 用户订单列表 -- 分页查询
     *
     * 1、支付成功的回调页
     *
     * memberOrder.html
     */
    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum, Model model){
        Map<String, Object> map = new HashMap<>();
        map.put("page", pageNum.toString());
        PageUtils page = orderService.queryPageWithItem(map);
        model.addAttribute("orders", page);
        System.out.println(JSON.toJSONString(page));
        return "memberOrder";
    }
}
