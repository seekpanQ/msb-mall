package com.msb.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.utils.PageUtils;
import com.msb.mall.product.entity.SpuInfoEntity;
import com.msb.mall.product.vo.OrderItemSpuInfoVO;
import com.msb.mall.product.vo.SpuInfoVO;

import java.util.List;
import java.util.Map;

/**
 * spu信息
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-19 22:59:19
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuInfoVO spuInfoVO);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void up(Long spuId);

    List<OrderItemSpuInfoVO> getOrderItemSpuInfoBySpuId(Long[] spuIds);
}

