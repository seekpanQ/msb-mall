package com.msb.mall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.msb.mall.order.vo.PayVo;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class AlipayTemplate {
    // 商户appid 沙箱账号: tklalf8880@sandbox.com
    public static String APPID = "";
    // 私钥 pkcs8格式的
    public static String RSA_PRIVATE_KEY = "";
    // 服务器异步通知页面路径 需http://或者https://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public static String notify_url = "http://order.msb.com/payed/notify";
    // 页面跳转同步通知页面路径 需http://或者https://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问 商户可以自定义同步跳转地址
    public static String return_url = "http://order.msb.com/orderPay/returnUrl";
    // 请求网关地址
    public static String URL = "https://openapi.alipay.com/gateway.do";
    // 编码
    public static String CHARSET = "UTF-8";
    // 返回格式
    public static String FORMAT = "json";
    // 支付宝公钥
    public static String ALIPAY_PUBLIC_KEY = "";
    // 日志记录目录
    public static String log_path = "/log";
    // RSA2
    public static String SIGNTYPE = "RSA2";

    public String pay(PayVo payVo) {
        // SDK 公共请求类，包含公共请求参数，以及封装了签名与验签，开发者无需关注签名与验签
        //实例化客户端
        AlipayClient alipayClient
                = new DefaultAlipayClient(URL
                , APPID
                , RSA_PRIVATE_KEY
                , FORMAT, CHARSET
                , ALIPAY_PUBLIC_KEY
                , SIGNTYPE);
        AlipayTradeWapPayRequest alipayTradeWapPayRequest = new AlipayTradeWapPayRequest();
        // 封装请求支付信息
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(payVo.getOut_trader_no());
        model.setSubject(payVo.getSubject());
        model.setTotalAmount(payVo.getTotal_amount());
        model.setBody(payVo.getBody());
        model.setTimeoutExpress("5000");
        model.setProductCode("111111");
        alipayTradeWapPayRequest.setBizModel(model);
        // 设置异步通知地址
        alipayTradeWapPayRequest.setNotifyUrl(notify_url);
        // 设置同步地址
        alipayTradeWapPayRequest.setReturnUrl(return_url);
        // form表单生产
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayTradeWapPayRequest).getBody();
            return form;
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }


}
