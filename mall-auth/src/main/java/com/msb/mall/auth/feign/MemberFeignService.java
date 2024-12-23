package com.msb.mall.auth.feign;

import com.msb.common.utils.R;
import com.msb.mall.auth.vo.LoginVo;
import com.msb.mall.auth.vo.SocialUser;
import com.msb.mall.auth.vo.UserRegisterVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 会员服务
 */
@FeignClient("mall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/register")
    R register(@RequestBody UserRegisterVo vo);

    @PostMapping("/member/member/login")
    R login(LoginVo loginVo);

    @RequestMapping("/member/member/oauth2/login")
    R socialLogin(@RequestBody SocialUser vo);


}
