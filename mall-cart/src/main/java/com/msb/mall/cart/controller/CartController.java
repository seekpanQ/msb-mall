package com.msb.mall.cart.controller;

import com.msb.mall.cart.service.ICartService;
import com.msb.mall.cart.vo.Cart;
import com.msb.mall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CartController {

    @Autowired
    private ICartService cartService;

    /**
     * 加入购物车
     *
     * @return
     */
    @GetMapping("/addCart")
    public String addCart(@RequestParam("skuId") Long skuId,
                          @RequestParam("num") Integer num, Model model) {
        CartItem item = null;
        try {
            item = cartService.addCart(skuId, num);
        } catch (Exception e) {
            e.printStackTrace();
        }
        model.addAttribute("item", item);
        return "success";
    }

    @GetMapping("/cart_list")
    public String queryCartList(Model model) {
        Cart cart = cartService.getCartList();
        model.addAttribute("cart", cart);
        return "cartList";
    }
}
