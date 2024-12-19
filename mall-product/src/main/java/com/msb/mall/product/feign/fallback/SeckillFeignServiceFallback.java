package com.msb.mall.product.feign.fallback;

import com.msb.common.exception.BizCodeEnum;
import com.msb.common.utils.R;
import com.msb.mall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillFeignServiceFallback implements SeckillFeignService {
    @Override
    public R getSeckillSessionBySkuId(Long skuId) {
        log.error("熔断降级....SeckillFeignService:{}", skuId);
        return R.error(BizCodeEnum.UNKNOW_EXCEPTION.getCode(), BizCodeEnum.UNKNOW_EXCEPTION.getMsg());
    }
}
