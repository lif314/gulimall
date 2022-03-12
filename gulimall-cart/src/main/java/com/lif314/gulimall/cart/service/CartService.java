package com.lif314.gulimall.cart.service;

import com.lif314.gulimall.cart.vo.Cart;
import com.lif314.gulimall.cart.vo.CartItem;

import java.util.concurrent.ExecutionException;

public interface CartService {

    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItem getCartItemRedis(Long skuId);

    Cart getCart();
}
