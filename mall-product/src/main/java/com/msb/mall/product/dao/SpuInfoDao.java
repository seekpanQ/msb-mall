package com.msb.mall.product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.product.entity.SpuInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-19 22:59:19
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    void updateSpuStatusUp(@Param("spuId") Long spuId, @Param("code") int code);
}
