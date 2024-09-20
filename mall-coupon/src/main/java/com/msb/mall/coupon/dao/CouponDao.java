package com.msb.mall.coupon.dao;

import com.msb.mall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-20 16:15:02
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
