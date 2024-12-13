package com.msb.mall.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.msb.mall.order.entity.OrderDetailEntity;
import com.msb.mall.order.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单项信息
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-20 15:39:16
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {

    IPage<OrderDetailEntity> getUserOrderList(IPage<OrderDetailEntity> page, @Param("memberId") Long memberId);
}
