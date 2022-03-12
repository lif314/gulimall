package com.lif314.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.lif314.common.constant.CartConstant;
import com.lif314.common.utils.R;
import com.lif314.gulimall.cart.feign.ProductFeignService;
import com.lif314.gulimall.cart.interceptor.CartInterceptor;
import com.lif314.gulimall.cart.service.CartService;
import com.lif314.gulimall.cart.to.SkuInfoVo;
import com.lif314.gulimall.cart.to.UserInfoTo;
import com.lif314.gulimall.cart.vo.Cart;
import com.lif314.gulimall.cart.vo.CartItem;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    // 线程池
    @Autowired
    ThreadPoolExecutor executor;

    /**
     * 添加购物车 -- 添加在Redis中
     *
     * @param skuId 商品id
     * @param num   商品数量
     * @return 购物项
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        // 先判断Redis中是否已经存在商品信息
        String res = (String) cartOps.get(skuId.toString());
        if(StringUtils.isEmpty(res)){
            // 2 添加新商品在购物车
            CartItem cartItem = new CartItem();
            // 异步编排
            CompletableFuture<Void> getSkuInfo = CompletableFuture.runAsync(() -> {
                // 远程获取商品信息
                R r = productFeignService.getSkuInfo(skuId);
                if (r.getCode() == 0) {
                    Object data = r.get("skuInfo");
                    SkuInfoVo skuInfoVo = JSON.parseObject(JSON.toJSONString(data), SkuInfoVo.class);
                    // 商品加入购物项
                    cartItem.setCheck(true);
                    cartItem.setCount(num);
                    cartItem.setImage(skuInfoVo.getSkuDefaultImg());
                    cartItem.setTitle(skuInfoVo.getSkuTitle());
                    cartItem.setSkuId(skuId);
                    cartItem.setPrice(skuInfoVo.getPrice());
                }
            }, executor);

            CompletableFuture<Void> getSaleAttrs = CompletableFuture.runAsync(() -> {
                // 远程查询属性信息
                // TODO 多个远程服务查询，可以放在线程池中
                List<String> saleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(saleAttrValues);
            }, executor);

            // 需要等异步处理结束后才能获取数据
            CompletableFuture.allOf(getSkuInfo, getSaleAttrs).get();

            // 保存在Redis中
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), s);
            return cartItem;
        }else{
            // 更新商品的数量
            CartItem item = JSON.parseObject(res, CartItem.class);
            item.setCount(item.getCount() + num);
            cartOps.put(item.getSkuId(), item);
            return item;
        }
    }

    @Override
    public CartItem getCartItemRedis(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(res, CartItem.class);
    }

    /**
     * 获取购物车所有数据
     *
     * TODO 由于用户信息放在LocalThread，随时随地都能获取，所以没有参数用户ID
     */
    @Override
    public Cart getCart() {
        // 获取用户数据
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        Cart cart = new Cart();
        // 判断用户是否登录
        if(userInfoTo.getUserId() != null){
            // 登录: 需要合并临时购物车和正式购物车

            // 获取正式的购物车
            String userIdKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItemsByUserId = getCartByKey(userIdKey);

            // 获取临时购物车
            String userKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCarts = getCartByKey(userKey);
            if(tempCarts != null && tempCarts.size() > 0){
                // 临时购物车有数据，需要合并
                // 将正式购物车中的数据映射为 <skuId, count>
                List<Map<Long, Integer>> maps = cartItemsByUserId.stream().map((item) -> {
                    Map<Long, Integer> userIdMap = new HashMap<>();
                    userIdMap.put(item.getSkuId(), item.getCount());
                    return userIdMap;
                }).collect(Collectors.toList());

            }
            // 合并完成
            cart.setItems(cartItemsByUserId);
            // 清除临时购物车 -- 这个应该可以删除hash中的数据
            redisTemplate.delete(userKey);
        }else {
            // 没有登录
            String userKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            cart.setItems(getCartByKey(userKey));
        }

        return cart;
    }

    /**
     * 根据key获取购物车中的数据
     * @param cartKey
     * @return
     */
    private List<CartItem> getCartByKey(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        List<CartItem> cartItems = new ArrayList<>();
        if(values != null && values.size() > 0){
            cartItems = values.stream().map((obj) -> {
                CartItem cartItem = JSON.parseObject(JSON.toJSONString(obj), CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
        }
        return cartItems;
    }


    // 封装对Redis的操作
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            // 已经登陆了
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
        } else {
            // 临时购物车
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
        }

        // 将商品信息存在Redis中
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        return hashOps;
    }


    /**
     *获取购物车： 登录前是临时购物车
     *      登陆后才是真的购物车
     * 判断是否登录：Session中是否存在相关的信息
     */


}