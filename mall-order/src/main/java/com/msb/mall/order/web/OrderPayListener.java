package com.msb.mall.order.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

@Controller
public class OrderPayListener {

    @RequestMapping("/payed/notify")
    public String handleAliPay(HttpServletRequest request) {
        System.out.println("----------------->支付成功的回调接口");
        Map<String, String[]> parameterMap = request.getParameterMap();
        Set<String> keys = parameterMap.keySet();
        for (String key : keys) {
            System.out.println(key + ":" + parameterMap.get(key));
        }
        return "success";
    }
}
