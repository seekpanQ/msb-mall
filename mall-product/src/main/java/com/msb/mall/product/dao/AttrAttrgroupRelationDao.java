package com.msb.mall.product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.product.entity.AttrAttrgroupRelationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性&属性分组关联
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-19 22:59:19
 */
@Mapper
public interface AttrAttrgroupRelationDao extends BaseMapper<AttrAttrgroupRelationEntity> {

    void removeBatchRelation(@Param("entityList") List<AttrAttrgroupRelationEntity> entityList);
}
