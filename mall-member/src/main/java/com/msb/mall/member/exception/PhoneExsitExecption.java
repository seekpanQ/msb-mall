package com.msb.mall.member.exception;

public class PhoneExsitExecption extends RuntimeException {

    public PhoneExsitExecption() {
        super("手机号存在");
    }
}
