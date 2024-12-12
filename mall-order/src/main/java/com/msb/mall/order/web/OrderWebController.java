package com.msb.mall.order.web;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse;
import com.msb.common.exception.NoStockExecption;
import com.msb.mall.order.service.OrderService;
import com.msb.mall.order.vo.OrderConfirmVo;
import com.msb.mall.order.vo.OrderResponseVO;
import com.msb.mall.order.vo.OrderSubmitVO;
import com.msb.mall.order.vo.PayVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderWebController {

    @Value("${alipay.returnUrl}")
    private String returnUrl;
    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("confirmVo", confirmVo);
        return "confirm";
    }

    @PostMapping("/orderSubmit")
    public String orderSubmit(OrderSubmitVO vo, Model model, RedirectAttributes redirectAttributes) {
        OrderResponseVO orderResponseVO = null;
        Integer code = 0;
        try {
            orderResponseVO = orderService.submitOrder(vo);
            code = orderResponseVO.getCode();
        } catch (NoStockExecption e) {
            code = 2;
        }
        if (code == 0) {
            model.addAttribute("orderResponseVO", orderResponseVO);
            // 表示下单操作成功
            return "pay";
        } else {
            System.out.println("code=" + code);
            String msg = "订单失败";
            if (code == 1) {
                msg = msg + ":重复提交";
            } else if (code == 2) {
                msg = msg + ":锁定库存失败";
            }
            //redirectAttributes.addAttribute("msg",msg);
            redirectAttributes.addFlashAttribute("msg", msg);
            return "redirect:http://order.msb.com/toTrade";
        }
    }

    @GetMapping("/orderPay/returnUrl")
    public String orderPay(@RequestParam(value = "orderSn", required = false) String orderSn
            , @RequestParam(value = "out_trade_no", required = false) String out_trade_no) {
        System.out.println("orderSn = " + out_trade_no);
        if (StringUtils.isNotBlank(orderSn)) {
            orderService.handleOrderComplete(orderSn);
        } else {
            orderService.handleOrderComplete(out_trade_no);
        }
        return "list";
    }

    /**
     * 获取订单相关信息
     * 然后跳转到支付页面  tklalf8880@sandbox.com
     *
     * @param orderSn
     * @return
     */
    @GetMapping(value = "/payOrder", produces = "text/html")
    @ResponseBody
    public String payOrder(@RequestParam("orderSn") String orderSn) {
        // 根据订单编号查询出相关的订单信息，封装到PayVO中
        PayVo payVo = orderService.getOrderPay(orderSn);
        AlipayTradePagePayResponse response;
        try {
            response = Factory.Payment.Page().pay(payVo.getSubject(), payVo.getOut_trader_no(),
                    payVo.getTotal_amount(), returnUrl);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        return response.getBody();
    }


}
