package com.msb.mall.auth.controller;

import com.msb.common.constant.SMSConstant;
import com.msb.common.exception.BizCodeEnum;
import com.msb.common.utils.R;
import com.msb.mall.auth.feign.ThirdPartyFeignService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThirdPartyFeignService thirdPartyFeignService;

    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendSmsCode(@RequestParam("phone") String phone) {
        // 防止60秒之内重复发送
        Object redisCode = redisTemplate.opsForValue().get(SMSConstant.SMS_CODE_PERFIX + phone);
        if (redisCode != null) {
            String s = redisCode.toString();
            if (StringUtils.isNotEmpty(s)) {
                long startTime = Long.parseLong(s.split("_")[1]);
                if (System.currentTimeMillis() - startTime <= 60000) {
                    // 说明验证码的发送间隔不足一分钟 提示
                    return R.error(BizCodeEnum.VALID_SMS_EXCEPTION.getCode(),
                            BizCodeEnum.VALID_SMS_EXCEPTION.getMsg());
                }
            }
        }
        // 生成随机的验证码 --》 把生成的验证码存储到Redis服务中 sms:code:13316995437  12345
        String code = UUID.randomUUID().toString().substring(0, 5);
        thirdPartyFeignService.sendSmsCode(phone, code);
        code = code + "_" + System.currentTimeMillis();
        System.out.println("code = " + code);
        redisTemplate.opsForValue().set(SMSConstant.SMS_CODE_PERFIX + phone, code, 10, TimeUnit.MINUTES);
        return R.ok();
    }

    @GetMapping("/login")
    public String login() {

        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "reg";
    }
}
