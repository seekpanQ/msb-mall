package com.msb.mall.coupon.controller;

import com.msb.common.dto.SkuReductionDTO;
import com.msb.common.utils.R;
import com.msb.mall.coupon.service.SkuFullReductionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品满减信息
 *
 * @author dpb
 * @email dengpbs@163.com
 * @date 2021-11-24 19:50:53
 */
@RestController
@RequestMapping("coupon/skufullreduction")
public class SkuFullReductionController {

    @Autowired
    private SkuFullReductionService skuFullReductionService;

    @PostMapping("/saveinfo")
    public R saveFullReductionInfo(@RequestBody SkuReductionDTO dto) {
        skuFullReductionService.saveSkuReduction(dto);
        return R.ok();
    }


}
