package com.msb.mall.product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.product.entity.SkuSaleAttrValueEntity;
import com.msb.mall.product.vo.SkuItemSaleAttrVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku销售属性&值
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-19 22:59:19
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    List<SkuItemSaleAttrVo> getSkuSaleAttrValueBySpuId(@Param("spuId") Long spuId);
}
