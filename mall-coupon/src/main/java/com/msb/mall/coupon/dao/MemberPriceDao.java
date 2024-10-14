package com.msb.mall.coupon.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.coupon.entity.MemberPriceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品会员价格
 * 
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-10-13 23:32:18
 */
@Mapper
public interface MemberPriceDao extends BaseMapper<MemberPriceEntity> {
	
}
