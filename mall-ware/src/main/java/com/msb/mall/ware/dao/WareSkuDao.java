package com.msb.mall.ware.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.ware.entity.WareSkuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-10-15 11:02:55
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    Long getSkuStock(@Param("skuId") Long skuId);

    List<WareSkuEntity> listHasStock(@Param("skuId") Long skuId);
    List<WareSkuEntity> listLockedStock(@Param("skuId") Long skuId);

    Integer lockSkuStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("count") Integer count);

    Integer releaseSkuStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("count") Integer count);
}
