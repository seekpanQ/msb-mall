package com.msb.mall.cart.controller;

import com.msb.common.vo.MemberVO;
import com.msb.mall.cart.interceptor.AuthInterceptor;
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

        MemberVO memberVO = AuthInterceptor.threadLocal.get();
        System.out.println(memberVO);

        return "success";
    }
}
