package com.msb.mall.order.feign;

import com.msb.common.utils.R;
import com.msb.mall.order.vo.WareSkuLockVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("mall-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVO vo);

    @PostMapping("/ware/waresku/release/order")
    public R orderReleaseStock(@RequestBody WareSkuLockVO vo);
}
