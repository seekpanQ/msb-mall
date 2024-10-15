package com.msb.mall.ware.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.ware.entity.PurchaseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购信息
 * 
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-10-15 11:02:55
 */
@Mapper
public interface PurchaseDao extends BaseMapper<PurchaseEntity> {
	
}
