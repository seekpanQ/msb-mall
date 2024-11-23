package com.msb.mall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.msb.common.constant.CartConstant;
import com.msb.common.utils.R;
import com.msb.common.vo.MemberVO;
import com.msb.mall.cart.feign.ProductFeignService;
import com.msb.mall.cart.interceptor.AuthInterceptor;
import com.msb.mall.cart.service.ICartService;
import com.msb.mall.cart.vo.Cart;
import com.msb.mall.cart.vo.CartItem;
import com.msb.mall.cart.vo.SkuInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class CartServiceImpl implements ICartService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private ThreadPoolExecutor executor;

    /**
     * 把商品添加到购物车中
     *
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws Exception
     */
    @Override
    public CartItem addCart(Long skuId, Integer num) throws ExecutionException, InterruptedException, Exception {
        BoundHashOperations<String, Object, Object> hashOperations = getCartKeyOperation();
        Object o = hashOperations.get(skuId.toString());
        // 如果Redis存储在商品的信息，那么我们只需要修改商品的数量就可以了
        if (o != null) {
            // 说明已经存在了这个商品那么修改商品的数量即可
            String json = (String) o;
            CartItem item = JSON.parseObject(json, CartItem.class);
            item.setCount(item.getCount() + num);
            hashOperations.put(skuId.toString(), JSON.toJSONString(item));
            return item;
        }

        CartItem item = new CartItem();
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            // 1.远程调用获取 商品信息
            R r = productFeignService.info(skuId);
            String skuInfoJSON = (String) r.get("skuInfoJSON");
            SkuInfoVo vo = JSON.parseObject(skuInfoJSON, SkuInfoVo.class);
            item.setCheck(true);
            item.setCount(num);
            item.setImage(vo.getSkuDefaultImg());
            item.setPrice(vo.getPrice());
            item.setSkuId(vo.getSkuId());
            item.setTitle(vo.getSkuTitle());
        }, executor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            // 2.获取商品的销售属性
            List<String> skuSaleAttrs = productFeignService.getSkuSaleAttrs(skuId);
            item.setSkuAttr(skuSaleAttrs);
        }, executor);
        CompletableFuture.allOf(future1, future2).get();
        // 3.把数据存储在Redis中
        String json = JSON.toJSONString(item);
        hashOperations.put(skuId.toString(), json);
        return item;
    }

    @Override
    public Cart getCartList() {
        BoundHashOperations<String, Object, Object> operation = getCartKeyOperation();
        Set<Object> keys = operation.keys();
        Cart cart = new Cart();
        List<CartItem> list = new ArrayList<>();
        for (Object k : keys) {
            String key = (String) k;
            Object o = operation.get(key);
            String json = (String) o;
            CartItem item = JSON.parseObject(json, CartItem.class);
            list.add(item);
        }
        cart.setItems(list);
        return cart;
    }

    private BoundHashOperations<String, Object, Object> getCartKeyOperation() {
        MemberVO memberVO = AuthInterceptor.threadLocal.get();
        String cartKey = CartConstant.CART_PERFIX + memberVO.getId();
        BoundHashOperations<String, Object, Object> hashOperations = redisTemplate.boundHashOps(cartKey);
        return hashOperations;
    }
}
