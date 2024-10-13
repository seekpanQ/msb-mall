package com.msb.mall.coupon.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.dto.SkuReductionDTO;
import com.msb.common.utils.PageUtils;
import com.msb.mall.coupon.dao.SkuFullReductionDao;
import com.msb.mall.coupon.entity.SkuFullReductionEntity;
import com.msb.mall.coupon.service.SkuFullReductionService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        return null;
    }

    /**
     * 保存 满减 折扣 会员价的相关信息
     *
     * @param dto
     */
    @Override
    public void saveSkuReduction(SkuReductionDTO dto) {
        // 5.3 保存满减信息，折扣，会员价
        // mall_sms: sms_sku_ladder sms_full_reduction sms_member_price
        // 1.折扣
    }
}
