package com.msb.mall.product.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("mall-coupon")
public interface CouponFeignService {


}
