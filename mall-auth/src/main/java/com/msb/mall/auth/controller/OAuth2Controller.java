package com.msb.mall.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuth2Controller {

    @RequestMapping("/oauth/weibo/success")
    public String weiboAuth(@RequestParam("code") String code) {

        return "";
    }
}
