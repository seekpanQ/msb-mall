package com.msb.mall.cart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {

    /**
     * 加入购物车
     *
     * @return
     */
    @GetMapping("/addCart")
    public String addCart(Model model) {

        return "success";
    }
}
