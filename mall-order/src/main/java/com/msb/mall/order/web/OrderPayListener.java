package com.msb.mall.order.web;

import com.alipay.easysdk.factory.Factory;
import com.msb.mall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OrderPayListener {
    @Autowired
    private OrderService orderService;

    /**
     * notify接口需要映射服务、花钱
     * 所以handleOrderComplete挪到retrunUrl中处理
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/payed/notify")
    public String handleAliPay(HttpServletRequest request) throws Exception {
        System.out.println("支付宝回调 notify");
        String tradeStatus = request.getParameter("trade_status");
        if (tradeStatus.trim().equals("TRADE_SUCCESS")) {
            Map<String, String> param = new HashMap<>();

            Map<String, String[]> parameterMap = request.getParameterMap();
            for (String name : parameterMap.keySet()) {
                param.put(name, request.getParameter(name));
            }

            if (Factory.Payment.Common().verifyNotify(param)) {
                System.out.println("通过支付宝的验证");
                String out_trade_no = param.get("out_trade_no");
                orderService.handleOrderComplete(out_trade_no);

                for (String name : param.keySet()) {
                    System.out.println("收到并且接受好的参数，");
                    System.out.println(name + "," + param.get(name));
                }
            } else {
                System.out.println("支付宝验证 不通过！");
            }

        }
        return "success";
    }
}
