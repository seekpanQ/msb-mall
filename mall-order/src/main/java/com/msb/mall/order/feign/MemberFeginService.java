package com.msb.mall.order.feign;

import com.msb.common.utils.R;
import com.msb.common.vo.MemberVO;
import com.msb.mall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("mall-member")
public interface MemberFeginService {

    @GetMapping("/member/memberreceiveaddress/{memberId}/address")
    List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);

    @RequestMapping("/member/memberreceiveaddress/getAddressById/{id}")
    MemberAddressVo getAddressById(@PathVariable("id") Long id);

    @RequestMapping("/member/member/updateIntegrationGrowth")
    public R updateIntegrationGrowth(@RequestBody MemberVO member);
}
