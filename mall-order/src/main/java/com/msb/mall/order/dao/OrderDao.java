package com.msb.mall.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-20 15:39:16
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    OrderEntity getOrderByOrderSn(@Param("orderSn") String orderSn);
}
