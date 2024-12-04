package com.msb.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.common.vo.MemberVO;
import com.msb.mall.order.dao.OrderDao;
import com.msb.mall.order.entity.OrderEntity;
import com.msb.mall.order.feign.CartFeginService;
import com.msb.mall.order.feign.MemberFeginService;
import com.msb.mall.order.interceptor.AuthInterceptor;
import com.msb.mall.order.service.OrderService;
import com.msb.mall.order.vo.MemberAddressVo;
import com.msb.mall.order.vo.OrderConfirmVo;
import com.msb.mall.order.vo.OrderItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MemberFeginService memberFeginService;
    @Autowired
    private CartFeginService cartFeginService;
    @Autowired
    private ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo vo = new OrderConfirmVo();
        MemberVO memberVO = AuthInterceptor.threadLocal.get();
        // 获取到 RequestContextHolder 的相关信息
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            // 同步主线程中的 RequestContextHolder
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 1.查询当前登录用户对应的会员的地址信息
            Long id = memberVO.getId();
            List<MemberAddressVo> address = memberFeginService.getAddress(id);
            vo.setAddress(address);
        }, executor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 2.查询购物车中选中的商品信息
            List<OrderItemVo> userCartItems = cartFeginService.getUserCartItems();
            vo.setItems(userCartItems);
        }, executor);

        try {
            CompletableFuture.allOf(future1, future2).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 3.计算订单的总金额和需要支付的总金额 VO自动计算
        return vo;
    }

}