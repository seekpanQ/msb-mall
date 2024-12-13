package com.msb.mall.cart.service;

import com.msb.mall.cart.vo.Cart;
import com.msb.mall.cart.vo.CartItem;
import com.msb.mall.cart.vo.CheckCartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 购物车的Service接口
 */
public interface ICartService {
    CartItem addCart(Long skuId, Integer num) throws ExecutionException, InterruptedException, Exception;

    Cart getCartList();

    List<CartItem> getUserCartItems();

    void checkCart(CheckCartVo[] checkCartVos);
}
