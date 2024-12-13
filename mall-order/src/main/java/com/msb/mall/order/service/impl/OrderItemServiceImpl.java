package com.msb.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.common.vo.MemberVO;
import com.msb.mall.order.dao.OrderDao;
import com.msb.mall.order.dao.OrderItemDao;
import com.msb.mall.order.entity.OrderEntity;
import com.msb.mall.order.entity.OrderItemEntity;
import com.msb.mall.order.interceptor.AuthInterceptor;
import com.msb.mall.order.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {
    @Autowired
    private OrderDao orderDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils getUserPage(Map<String, Object> params) {
        MemberVO memberVO = AuthInterceptor.threadLocal.get();
        Long id = memberVO.getId();
        List<OrderEntity> orderEntityList
                = orderDao.selectList(new QueryWrapper<OrderEntity>().eq("member_id", id));
        List<String> orderSns = orderEntityList.stream().map(item -> {
            return item.getOrderSn();
        }).collect(Collectors.toList());

        QueryWrapper wrapper = new QueryWrapper<OrderItemEntity>();
        wrapper.in("order_sn", orderSns);
        IPage<OrderItemEntity> page = this.page(new Query<OrderItemEntity>().getPage(params), wrapper);
        PageUtils pageUtils = new PageUtils(page);
        return pageUtils;


    }

}