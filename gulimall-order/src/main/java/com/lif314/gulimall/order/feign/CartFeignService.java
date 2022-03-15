package com.lif314.gulimall.order.feign;

import com.lif314.gulimall.order.to.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


@FeignClient("gulimall-cart")
public interface CartFeignService {
    /**
     * 获取当前用户选中的购物项
     */
    @GetMapping("/currentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItems();
}
