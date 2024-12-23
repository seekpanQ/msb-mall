package com.msb.mall.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.utils.PageUtils;
import com.msb.mall.order.entity.OrderDetailEntity;
import com.msb.mall.order.entity.OrderItemEntity;

import java.util.Map;

/**
 * 订单项信息
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-20 15:39:16
 */
public interface OrderItemService extends IService<OrderItemEntity> {

    PageUtils queryPage(Map<String, Object> params);

    IPage<OrderDetailEntity> getUserPage(Map<String, Object> params);
}

