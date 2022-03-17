package com.lif314.gulimall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.constant.OrderConstant;
import com.lif314.common.to.MemberRespTo;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;
import com.lif314.common.utils.R;
import com.lif314.gulimall.order.dao.OrderDao;
import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.entity.OrderItemEntity;
import com.lif314.gulimall.order.feign.CartFeignService;
import com.lif314.gulimall.order.feign.MemberFeignService;
import com.lif314.gulimall.order.feign.ProductFeignService;
import com.lif314.gulimall.order.feign.WareFeignService;
import com.lif314.gulimall.order.intercepter.LoginUserInterceptor;
import com.lif314.gulimall.order.service.OrderItemService;
import com.lif314.gulimall.order.service.OrderService;
import com.lif314.gulimall.order.to.*;
import com.lif314.gulimall.order.vo.OrderConfirmVo;
import com.lif314.gulimall.order.vo.OrderSubmitVo;
import com.lif314.gulimall.order.vo.SubmitOrderRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    /**
     * 使用ThreadLocal共享页面订单数据，这样可以避免多次传参
     */
    private static final ThreadLocal<OrderSubmitVo> submitOrderThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderItemService orderItemService;


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

        // TODO 5、防止重复提交下单请求 -- 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        // 防重令牌需要分发给服务器和页面，然后页面发送请求时带着令牌进行比对
        orderConfirmVo.setOrderToken(token);  // 页面
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespTo.getId(), token, 30, TimeUnit.MINUTES);  // Redis

        return orderConfirmVo;
    }


    /**
     * 去创建订单，验令牌，验价格，锁库存。。。。
     */
    @Transactional
    @Override
    public SubmitOrderRespVo submitOrder(OrderSubmitVo submitVo) {
        // 共享再threadLocal中
        submitOrderThreadLocal.set(submitVo);

        // 从拦截器中获取当前登录的用户
        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();

        SubmitOrderRespVo respVo = new SubmitOrderRespVo();

        // 1. 验证令牌【对比和删除必须保证原子性】
        String orderToken = submitVo.getOrderToken();
        String script = "if redis.call(`get`, KEY[1]) == ARGV[1] then return redis.call(`del`, KEY[1]) else return 0 end";
        // 返回值，0 失败   1成功
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespTo.getId(), orderToken));
        if (result == 0L) {
            // 验证失败
            respVo.setCode(1);
            return respVo;
        } else {
            // 验证成功
            // 1、创建订单
            OrderCreateTo order = createOrder();
            // 2、验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = submitVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 3、对比成功--保存订单到数据库中
                saveOrder(order);

                // 4、锁定库存 -- 只要有异常回滚订单数据
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<SkuItemLockTo> skuItemLockTos = order.getOrderItems().stream().map((item) -> {
                    SkuItemLockTo skuItemLockTo = new SkuItemLockTo();
                    skuItemLockTo.setSkuId(item.getSkuId());
                    skuItemLockTo.setCount(item.getSkuQuantity());
                    return skuItemLockTo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(skuItemLockTos);

                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if(r.getCode() == 0){
                    // 库存锁定成功
                }else {
                    // 失败
                }

                respVo.setOrder(order.getOrder());
                respVo.setCode(1);

            } else {
                respVo.setCode(1);
                return respVo;
            }

            return respVo;
        }
//        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespTo.getId());
//        if (orderToken != null && orderToken.equals(redisToken)) {
//            // 令牌验证通过
//            // 业务逻辑
//            redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespTo.getId());
//        }
    }

    /**
     * 保存订单数据
     */
    private void saveOrder(OrderCreateTo order) {
        // 保存订单数据
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        // 保存订单项数据
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    /**
     * 创建订单
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();

        // 1、生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity order = buildOrder(orderSn);
        createTo.setOrder(order);

        // 2、获取所有订单项数据
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        createTo.setOrderItems(orderItemEntities);

        // 3、验价
        computePrice(order, orderItemEntities);

        return createTo;
    }

    // 计算价格相关
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        // 1.订单总额、促销总金额、优惠券总金额、积分优惠总金额
        BigDecimal total = new BigDecimal(0);
        BigDecimal coupon = new BigDecimal(0);
        BigDecimal promotion = new BigDecimal(0);
        BigDecimal integration = new BigDecimal(0);
        // 2.积分、成长值
        Integer giftIntegration = 0;
        Integer giftGrowth = 0;
        for (OrderItemEntity itemEntity : orderItemEntities) {
            total = total.add(itemEntity.getRealAmount());// 订单总额
            coupon = coupon.add(itemEntity.getCouponAmount());// 促销总金额
            promotion = promotion.add(itemEntity.getPromotionAmount());// 优惠券总金额
            integration = integration.add(itemEntity.getIntegrationAmount());// 积分优惠总金额
            giftIntegration = giftIntegration + itemEntity.getGiftIntegration();// 积分
            giftGrowth = giftGrowth + itemEntity.getGiftGrowth();// 成长值
        }
        orderEntity.setTotalAmount(total);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);

        // 设置积分等信息
        orderEntity.setIntegration(giftIntegration);// 总积分信息
        orderEntity.setGrowth(giftGrowth);// 总成长值

        // 3.应付总额
        orderEntity.setPayAmount(orderEntity.getTotalAmount().add(orderEntity.getFreightAmount()));// 订单总额 +　运费
    }

    /**
     * 构建订单项列表
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        // 最后确定每一个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> collect = currentUserCartItems.stream().map(this::buildOrderItem).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * 构建订单项
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        // 1、订单信息：订单号
        // 2、商品SPU信息
        Long skuId = cartItem.getSkuId();
        R spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        if (spuInfo.getCode() == 0) {
            // 获取成功
            Object data = spuInfo.get("data");
            String s = JSON.toJSONString(data);
            SpuInfoTo spuInfoTo = JSON.parseObject(s, SpuInfoTo.class);
            orderItemEntity.setSpuId(spuInfoTo.getId());
            orderItemEntity.setSpuName(spuInfoTo.getSpuName());
            orderItemEntity.setSpuBrand(spuInfoTo.getBrandId().toString());
            orderItemEntity.setCategoryId(spuInfoTo.getCatalogId());
        }

        // 3、商品SKU信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        orderItemEntity.setSkuQuantity(cartItem.getCount());

        // TODO 4、优惠信息

        // 5、积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        // TODO 6、订单项的价格信息
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        // 当前订单项的实际价格
        BigDecimal originPrice = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = originPrice.subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }

    /**
     * 构建订单
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();
        OrderEntity order = new OrderEntity();
        order.setOrderSn(orderSn);
        // 设置会员信息
        order.setMemberId(memberRespTo.getId());
        OrderSubmitVo orderSubmitVo = submitOrderThreadLocal.get();
        // 2、远程获取收获信息
        R fare = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if (fare.getCode() == 0) {
            // 获取成功
            Object data = fare.get("data");
            String s = JSON.toJSONString(data);
            FareTo fareTo = JSON.parseObject(s, FareTo.class);
            MemberAddressTo address = fareTo.getAddress();
            // 运费信息
            order.setFreightAmount(fareTo.getFare());
            // 收获地址信息
            order.setReceiverCity(address.getCity());
            order.setReceiverDetailAddress(address.getDetailAddress());
            order.setReceiverName(address.getName());
            order.setReceiverPhone(address.getPhone());
            order.setReceiverPostCode(address.getPostCode());
            order.setReceiverProvince(address.getProvince());
            order.setReceiverRegion(address.getRegion());
        }

        // 设置订单相关状态信息
        order.setStatus(OrderConstant.OrderStatusEnum.CREATE_NEW.getCode());
        order.setDeleteStatus(0); // 0代表未删除
        return order;
    }

}
