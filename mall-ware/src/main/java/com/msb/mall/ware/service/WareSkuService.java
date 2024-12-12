package com.msb.mall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.dto.SkuHasStockDto;
import com.msb.common.utils.PageUtils;
import com.msb.mall.ware.entity.WareSkuEntity;
import com.msb.mall.ware.vo.WareSkuLockVO;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-10-15 11:02:55
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockDto> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVO vo);

    Boolean orderReleaseStock(WareSkuLockVO vo);
}

