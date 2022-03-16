package com.lif314.gulimall.order.service.impl;

import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.to.MemberRespTo;
import com.lif314.gulimall.order.feign.CartFeignService;
import com.lif314.gulimall.order.feign.MemberFeignService;
import com.lif314.gulimall.order.intercepter.LoginUserInterceptor;
import com.lif314.gulimall.order.to.MemberAddressTo;
import com.lif314.gulimall.order.to.OrderItemVo;
import com.lif314.gulimall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.order.dao.OrderDao;
import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.service.OrderService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 订单确认页数据
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();
        // 主线程  RequestContextHolder -- 使用ThreadLocal共享数据
        // 获取之前的请求信息，每一个请求都应该共享数据
        RequestAttributes mainThreadRequest = RequestContextHolder.getRequestAttributes();

        // 使用异步编排
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            // 子线程
            RequestContextHolder.setRequestAttributes(mainThreadRequest);
            // 1、远程查询收获地址信息
            List<MemberAddressTo> memberAddress = memberFeignService.getMemberAddress(memberRespTo.getId());
            orderConfirmVo.setAddress(memberAddress);
        }, executor);


        CompletableFuture<Void> getItemsFuture = CompletableFuture.runAsync(() -> {
            // 子线程
            RequestContextHolder.setRequestAttributes(mainThreadRequest);
            // 2、远程查询购物车中的购物项
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            orderConfirmVo.setItems(currentUserCartItems);
        }, executor);

        // 3、查询用户积分
        Integer integration = memberRespTo.getIntegration();
        orderConfirmVo.setInteration(integration);

        // 4、其它属性自动计算

        CompletableFuture.allOf(getItemsFuture, getAddressFuture).get();

        // TODO 5、防止重复提交下单请求
        return orderConfirmVo;
    }

}
