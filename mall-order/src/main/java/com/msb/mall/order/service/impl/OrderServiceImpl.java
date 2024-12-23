package com.msb.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.constant.CartConstant;
import com.msb.common.constant.OrderConstant;
import com.msb.common.dto.SeckillOrderDto;
import com.msb.common.exception.NoStockExecption;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.common.utils.R;
import com.msb.common.vo.MemberVO;
import com.msb.mall.order.dao.OrderDao;
import com.msb.mall.order.dto.OrderCreateTO;
import com.msb.mall.order.entity.OrderEntity;
import com.msb.mall.order.entity.OrderItemEntity;
import com.msb.mall.order.feign.CartFeginService;
import com.msb.mall.order.feign.MemberFeginService;
import com.msb.mall.order.feign.ProductService;
import com.msb.mall.order.feign.WareFeignService;
import com.msb.mall.order.interceptor.AuthInterceptor;
import com.msb.mall.order.service.OrderItemService;
import com.msb.mall.order.service.OrderService;
import com.msb.mall.order.utils.OrderMsgProducer;
import com.msb.mall.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MemberFeginService memberFeginService;
    @Autowired
    private CartFeginService cartFeginService;
    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private WareFeignService wareFeignService;
    @Autowired
    private OrderMsgProducer orderMsgProducer;

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

        // 4.生成防重的Token
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        // 我们需要把这个Token信息存储在Redis中
        // order:token:用户编号
        redisTemplate.opsForValue().set(OrderConstant.ORDER_TOKEN_PREFIX + ":" + memberVO.getId(), token);
        // 然后我们需要将这个Token绑定在响应的数据对象中
        vo.setOrderToken(token);
        return vo;
    }

    /**
     * seata分布式事务配置
     * seata-server启动地址:
     * E:\study\mashibing_fee\code\seata-server-1.3.0\seata\conf
     *
     * @param vo
     * @return
     */
    @GlobalTransactional
    @Transactional
    @Override
    public OrderResponseVO submitOrder(OrderSubmitVO vo) {
        // 需要返回响应的对象
        OrderResponseVO responseVO = new OrderResponseVO();
        // 获取当前登录的用户信息
        MemberVO memberVO = (MemberVO) AuthInterceptor.threadLocal.get();
        // 1.验证是否重复提交  保证Redis中的token 的查询和删除是一个原子性操作
        String key = OrderConstant.ORDER_TOKEN_PREFIX + ":" + memberVO.getId();
        String script = "if redis.call('get',KEYS[1])==ARGV[1] then redis.call('del',KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class)
                , Arrays.asList(key)
                , vo.getOrderToken());
        if (result != null && result == 0) {
            // 表示验证失败 说明是重复提交
            responseVO.setCode(1);
            return responseVO;
        }

        // 2.创建订单和订单项信息
        OrderCreateTO orderCreateTO = createOrder(vo);
        responseVO.setOrderEntity(orderCreateTO.getOrderEntity());

        // 3.保存订单信息
        saveOrder(orderCreateTO);

        // 4.锁定库存信息
        // 订单号  SKU_ID  SKU_NAME 商品数量
        // 封装 WareSkuLockVO 对象
        lockWareSkuStock(responseVO, orderCreateTO);
        // int i = 1 / 0;
        // 订单成功后需要给 消息中间件发送延迟30分钟的关单消息
        orderMsgProducer.sendOrderMessage(orderCreateTO.getOrderEntity().getOrderSn());
        return responseVO;
    }

    /**
     * 在service中调用自身的其他事务方法的时候，事务的传播行为会失效
     * 因为会绕过代理对象的处理
     * 测试事务bc失效的问题
     */
    @Transactional
    @Override
    public void testTranscationPropagation() {
        OrderServiceImpl o = (OrderServiceImpl) AopContext.currentProxy();
        o.b();
        o.c();

        int a = 1 / 0;
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        // 根据订单号查询相关的订单信息
        OrderEntity orderEntity = this.getBaseMapper().getOrderByOrderSn(orderSn);
        // 通过订单信息封装 PayVO对象
        PayVo payVo = new PayVo();
        payVo.setOut_trader_no(orderSn);
        payVo.setTotal_amount(orderEntity.getTotalAmount().setScale(2, RoundingMode.UP).toString());
        // 订单名称和订单描述
        payVo.setSubject(orderEntity.getOrderSn());
        payVo.setBody(orderEntity.getOrderSn());
        return payVo;
    }

    @GlobalTransactional
    @Override
    public void handleOrderComplete(String orderSn) {
        // 1.更新订单状态
        this.updateOrderStatus(orderSn, OrderConstant.OrderStateEnum.TO_SEND_GOODS.getCode());

        // 2.更新库存信息 库存数量递减
        this.updateWareSku(orderSn);

        // 3.购物车中的已经支付的商品移除
        this.removePayedCartList(orderSn);

        // 4.更新会员积分 ....
        this.updateMemberIntegrationGrowth(orderSn);
    }

    /**
     * 快速完成订单的处理  秒杀活动
     *
     * @param seckillOrderDto
     */
    @Transactional
    @Override
    public void quickCreateOrder(SeckillOrderDto seckillOrderDto) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrderDto.getOrderSN());
        orderEntity.setStatus(OrderConstant.OrderStateEnum.FOR_THE_PAYMENT.getCode());
        orderEntity.setMemberId(seckillOrderDto.getMemberId());
        orderEntity.setCreateTime(new Date());
        orderEntity.setTotalAmount(seckillOrderDto.getSeckillPrice().multiply(new BigDecimal(seckillOrderDto.getNum())));
        this.save(orderEntity);
        // TODO 根据SKUID查询对应的SKU信息和SPU信息
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrderDto.getOrderSN());
        orderItemEntity.setSkuPrice(seckillOrderDto.getSeckillPrice());
        orderItemEntity.setSkuId(seckillOrderDto.getSkuId());
        orderItemEntity.setRealAmount(seckillOrderDto.getSeckillPrice().multiply(new BigDecimal(seckillOrderDto.getNum())));
        orderItemEntity.setSkuQuantity(seckillOrderDto.getNum());
        orderItemService.save(orderItemEntity);
    }

    /**
     * 更新会员积分和成长值
     *
     * @param orderSn
     */
    private void updateMemberIntegrationGrowth(String orderSn) {
        List<OrderItemEntity> orderItemEntities
                = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        MemberVO memberVO = AuthInterceptor.threadLocal.get();
        Integer integration = 0;
        Integer growth = 0;
        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            Integer giftIntegration = orderItemEntity.getGiftIntegration();
            Integer skuQuantity = orderItemEntity.getSkuQuantity();
            integration = integration + (giftIntegration * skuQuantity);
            Integer giftGrowth = orderItemEntity.getGiftGrowth();
            growth = growth + (giftGrowth * skuQuantity);
        }
        memberVO.setIntegration(integration);
        memberVO.setGrowth(growth);
        memberFeginService.updateIntegrationGrowth(memberVO);
    }

    /**
     * 购物车中的已经支付的商品移除
     *
     * @param orderSn
     */
    private void removePayedCartList(String orderSn) {
        List<OrderItemEntity> orderItemEntities
                = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        MemberVO memberVO = AuthInterceptor.threadLocal.get();
        String cartKey = CartConstant.CART_PERFIX + memberVO.getId();
        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            Long skuId = orderItemEntity.getSkuId();
            redisTemplate.boundHashOps(cartKey).delete(String.valueOf(skuId));
        }
    }

    private void updateWareSku(String orderSn) {
        List<OrderItemEntity> orderItemEntities
                = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        List<OrderItemVo> orderItemVos = orderItemEntities.stream().map(item -> {
            OrderItemVo orderItemVo = new OrderItemVo();
            BeanUtils.copyProperties(item, orderItemVo);
            orderItemVo.setImage(item.getSkuPic());
            orderItemVo.setTitle(item.getSkuName());
            orderItemVo.setCount(item.getSkuQuantity());
            orderItemVo.setPrice(item.getSkuPrice());
            orderItemVo.setTotalPrice(item.getSkuPrice().multiply(new BigDecimal(item.getSkuQuantity())));
            return orderItemVo;
        }).collect(Collectors.toList());
        WareSkuLockVO wareSkuLockVO = new WareSkuLockVO();
        wareSkuLockVO.setOrderSN(orderSn);
        wareSkuLockVO.setItems(orderItemVos);
        // 远程释放库存的操作
        R r = wareFeignService.orderReleaseStock(wareSkuLockVO);
    }

    /**
     * 更新订单的状态信息
     *
     * @param orderSn
     * @param status
     */
    @Override
    public void updateOrderStatus(String orderSn, Integer status) {
        this.getBaseMapper().updateOrderStatus(orderSn, status);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void b() {
        OrderEntity entity = new OrderEntity();
        entity.setId(9l);
        entity.setTotalAmount(new BigDecimal(1000));
        this.updateById(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void c() {
        OrderEntity entity = new OrderEntity();
        entity.setId(10l);
        entity.setStatus(2);
        this.updateById(entity);
    }

    /**
     * 锁定库存的方法
     *
     * @param responseVO
     * @param orderCreateTO
     */
    private void lockWareSkuStock(OrderResponseVO responseVO, OrderCreateTO orderCreateTO) {
        WareSkuLockVO wareSkuLockVO = new WareSkuLockVO();
        wareSkuLockVO.setOrderSN(orderCreateTO.getOrderEntity().getOrderSn());
        List<OrderItemVo> orderItemVos = orderCreateTO.getOrderItemEntitys().stream().map(item -> {
            OrderItemVo itemVo = new OrderItemVo();
            itemVo.setSkuId(item.getSkuId());
            itemVo.setTitle(item.getSkuName());
            itemVo.setCount(item.getSkuQuantity());
            return itemVo;
        }).collect(Collectors.toList());
        wareSkuLockVO.setItems(orderItemVos);
        // 远程锁库存的操作
        R r = wareFeignService.orderLockStock(wareSkuLockVO);
        if (r.getCode() == 0) {
            // 表示锁定库存成功
            responseVO.setCode(0);
        } else {
            // 表示锁定库存失败
            responseVO.setCode(2);// 表示库存不足，锁定失败
            throw new NoStockExecption(1000l);
        }

    }

    /**
     * 生成订单数据
     *
     * @param orderCreateTO
     */
    private void saveOrder(OrderCreateTO orderCreateTO) {
        OrderEntity orderEntity = orderCreateTO.getOrderEntity();
        orderService.save(orderEntity);
        // 2.订单项数据
        List<OrderItemEntity> orderItemEntitys = orderCreateTO.getOrderItemEntitys();
        orderItemService.saveBatch(orderItemEntitys);
    }

    /**
     * 创建订单的方法
     *
     * @param vo
     * @return
     */
    private OrderCreateTO createOrder(OrderSubmitVO vo) {
        OrderCreateTO createTO = new OrderCreateTO();
        OrderEntity orderEntity = buildOrder(vo);
        createTO.setOrderEntity(orderEntity);
        // 创建OrderItemEntity 订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderEntity.getOrderSn());
        // 根据订单项计算出支付总额
        BigDecimal totalAmount = new BigDecimal(0);
        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            BigDecimal total
                    = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity()));
            totalAmount = totalAmount.add(total);
        }
        orderEntity.setTotalAmount(totalAmount);
        orderEntity.setPayAmount(totalAmount);
        createTO.setOrderItemEntitys(orderItemEntities);
        return createTO;
    }

    /**
     * 通过购物车中选中的商品来创建对应的购物项信息
     *
     * @param orderSN
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSN) {
        List<OrderItemEntity> orderItemEntitys = new ArrayList<>();
        // 获取购物车中的商品信息 选中的
        List<OrderItemVo> userCartItems = cartFeginService.getUserCartItems();
        if (userCartItems != null && userCartItems.size() > 0) {
            // 统一根据SKUID查询出对应的SPU的信息
            List<Long> spuIds = new ArrayList<>();
            for (OrderItemVo userCartItem : userCartItems) {
                if (!spuIds.contains(userCartItem.getSpuId())) {
                    spuIds.add(userCartItem.getSpuId());
                }
            }
            Long[] spuIdsArray = new Long[spuIds.size()];
            spuIdsArray = spuIds.toArray(spuIdsArray);
            // 远程调用商品服务获取到对应的SPU信息
            List<OrderItemSpuInfoVO> spuInfos = productService.getOrderItemSpuInfoBySpuId(spuIdsArray);
            Map<Long, OrderItemSpuInfoVO> map
                    = spuInfos.stream().collect(Collectors.toMap(OrderItemSpuInfoVO::getId, item -> item));
            for (OrderItemVo userCartItem : userCartItems) {
                // 获取到商品信息对应的 SPU信息
                OrderItemSpuInfoVO spuInfo = map.get(userCartItem.getSpuId());
                OrderItemEntity orderItemEntity = buildOrderItem(userCartItem, spuInfo);
                // 绑定对应的订单编号
                orderItemEntity.setOrderSn(orderSN);
                orderItemEntitys.add(orderItemEntity);
            }
        }
        return orderItemEntitys;
    }

    /**
     * 根据一个购物车中的商品创建对应的 订单项
     *
     * @param userCartItem
     * @param spuInfo
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo userCartItem, OrderItemSpuInfoVO spuInfo) {
        OrderItemEntity entity = new OrderItemEntity();
        // SKU信息
        entity.setSkuId(userCartItem.getSkuId());
        entity.setSkuName(userCartItem.getTitle());
        entity.setSkuPic(userCartItem.getImage());
        entity.setSkuQuantity(userCartItem.getCount());
        List<String> skuAttr = userCartItem.getSkuAttr();
        String skuAttrStr = StringUtils.collectionToDelimitedString(skuAttr, ",");
        entity.setSkuAttrsVals(skuAttrStr);
        entity.setSkuPrice(userCartItem.getPrice());
        // SPU信息
        entity.setSpuId(spuInfo.getId());
        entity.setSpuName(spuInfo.getSpuName());
        entity.setSpuBrand(spuInfo.getBrandName());
        entity.setSpuPic(spuInfo.getImg());
        entity.setCategoryId(spuInfo.getCatalogId());
        // 优惠信息 忽略
        // 积分信息
        entity.setGiftGrowth(userCartItem.getPrice().intValue());
        entity.setGiftIntegration(userCartItem.getPrice().intValue());
        return entity;
    }

    private OrderEntity buildOrder(OrderSubmitVO vo) {
        // 创建OrderEntity
        OrderEntity orderEntity = new OrderEntity();
        // 创建订单编号
        String orderSn = IdWorker.getTimeId();
        orderEntity.setOrderSn(orderSn);
        MemberVO memberVO = AuthInterceptor.threadLocal.get();
        // 设置会员相关的信息
        orderEntity.setMemberId(memberVO.getId());
        orderEntity.setMemberUsername(memberVO.getUsername());
        // 根据收获地址ID获取收获地址的详细信息
        MemberAddressVo memberAddressVo = memberFeginService.getAddressById(vo.getAddrId());
        orderEntity.setReceiverCity(memberAddressVo.getCity());
        orderEntity.setReceiverProvince(memberAddressVo.getProvince());
        orderEntity.setReceiverDetailAddress(memberAddressVo.getDetailAddress());
        orderEntity.setReceiverName(memberAddressVo.getName());
        orderEntity.setReceiverPhone(memberAddressVo.getPhone());
        orderEntity.setReceiverPostCode(memberAddressVo.getPostCode());
        orderEntity.setReceiverRegion(memberAddressVo.getRegion());
        orderEntity.setCreateTime(new Date());
        // 订单总额
        // 设置订单的状态
        orderEntity.setStatus(OrderConstant.OrderStateEnum.FOR_THE_PAYMENT.getCode());
        return orderEntity;
    }

}