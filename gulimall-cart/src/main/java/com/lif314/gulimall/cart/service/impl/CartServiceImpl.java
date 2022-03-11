package com.lif314.gulimall.cart.service.impl;

import com.lif314.gulimall.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service("CartService")
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate RedisTemplate;


    /**
     *获取购物车： 登录前是临时购物车
     *      登陆后才是真的购物车
     * 判断是否登录：Session中是否存在相关的信息
     */





}
