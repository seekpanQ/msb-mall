package com.msb.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.utils.PageUtils;
import com.msb.mall.product.entity.BrandEntity;

import java.util.Map;

/**
 * 品牌
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-19 22:59:19
 */
public interface BrandService extends IService<BrandEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void updateDetail(BrandEntity brand);
}

