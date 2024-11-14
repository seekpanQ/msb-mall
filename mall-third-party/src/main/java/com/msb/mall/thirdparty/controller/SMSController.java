package com.msb.mall.thirdparty.controller;

import com.msb.common.utils.R;
import com.msb.mall.thirdparty.utils.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
public class SMSController {

    @Autowired
    private SmsComponent smsComponent;


    /**
     * 调用短信服务商提供的短信API发送短信
     *
     * @param phone
     * @param code
     * @return
     */
    @GetMapping("/sms/sendCode")
    public R sendSmsCode(@RequestParam("phone") String phone, @RequestParam("code") String code) throws ExecutionException, InterruptedException {
        smsComponent.sendSmsCode(phone, code);
        return R.ok();
    }
}
