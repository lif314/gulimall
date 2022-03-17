package com.lif314.gulimall.order.web;


import com.lif314.gulimall.order.service.OrderService;
import com.lif314.gulimall.order.vo.OrderConfirmVo;
import com.lif314.gulimall.order.vo.OrderSubmitVo;
import com.lif314.gulimall.order.vo.SubmitOrderRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.concurrent.ExecutionException;

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
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        // 获取选中的商品数据
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", orderConfirmVo);
        return "confirm";
    }

    /**
     * 提交订单
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo submitVo){

        // 去创建订单，验令牌，验价格，锁库存。。。。

        SubmitOrderRespVo respVo=  orderService.submitOrder(submitVo);

        if(respVo.getCode() == 0){
            // 下单成功来到支付选项
            return "redirect:http://order.feihong.com/pay.html";
        }else{
            // 下单失败回到订单确认页重新确认订单信息
            return "redirect:http://order.feihong.com/toTrade";
        }
    }



}
