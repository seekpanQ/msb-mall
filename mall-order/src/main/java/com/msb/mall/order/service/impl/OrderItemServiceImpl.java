package com.msb.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.common.vo.MemberVO;
import com.msb.mall.order.dao.OrderItemDao;
import com.msb.mall.order.entity.OrderDetailEntity;
import com.msb.mall.order.entity.OrderItemEntity;
import com.msb.mall.order.interceptor.AuthInterceptor;
import com.msb.mall.order.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {
    @Autowired
    private OrderItemDao orderItemDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public IPage<OrderDetailEntity> getUserPage(Map<String, Object> params) {
        MemberVO memberVO = AuthInterceptor.threadLocal.get();
        Long id = memberVO.getId();
        IPage<OrderDetailEntity> page = new Page<>();
        IPage<OrderDetailEntity> result = orderItemDao.getUserOrderList(page, id);
        return result;
    }

}