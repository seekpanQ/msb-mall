package com.msb.mall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.dto.SkuHasStockDto;
import com.msb.common.exception.NoStockExecption;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.common.utils.R;
import com.msb.mall.ware.dao.WareSkuDao;
import com.msb.mall.ware.entity.WareSkuEntity;
import com.msb.mall.ware.feign.ProductFeignService;
import com.msb.mall.ware.service.WareSkuService;
import com.msb.mall.ware.vo.OrderItemVo;
import com.msb.mall.ware.vo.WareSkuLockVO;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuDao wareSkuDao;
    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            wrapper.eq("sku_id", key).or()
                    .eq("ware_id", key);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 入库操作
     *
     * @param skuId  商品编号
     * @param wareId 仓库编号
     * @param skuNum 采购商品的数量
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 判断是否有改商品和仓库的入库记录
        List<WareSkuEntity> list = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>()
                .eq("sku_id", skuId)
                .eq("ware_id", wareId));
        if (list == null || list.size() == 0) {
            // 如果没有就新增商品库存记录
            WareSkuEntity entity = new WareSkuEntity();
            entity.setSkuId(skuId);
            entity.setWareId(wareId);
            entity.setStock(skuNum);
            entity.setStockLocked(0);
            try {
                // 动态的设置商品的名称
                R info = productFeignService.info(skuId);// 通过Feign远程调用商品服务的接口
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    entity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();// 插入商品库存记录
            }
            wareSkuDao.insert(entity);
        } else {
            // 如果有就更新库存
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }

    }

    @Override
    public List<SkuHasStockDto> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockDto> list = skuIds.stream().map(
                skuId -> {
                    Long count = baseMapper.getSkuStock(skuId);
                    SkuHasStockDto dto = new SkuHasStockDto();
                    dto.setSkuId(skuId);
                    dto.setHasStock(count > 0);
                    return dto;
                }
        ).collect(Collectors.toList());
        return list;

    }

    /**
     * 锁定库存的操作
     *
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVO vo) {
        List<OrderItemVo> items = vo.getItems();
        // 首先找到具有库存的仓库
        List<SkuWareHasStock> collect = items.stream().map(item -> {
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            skuWareHasStock.setSkuId(item.getSkuId());
            List<WareSkuEntity> wareSkuEntities = this.baseMapper.listHasStock(item.getSkuId());
            skuWareHasStock.setWareSkuEntities(wareSkuEntities);
            skuWareHasStock.setNum(item.getCount());
            return skuWareHasStock;
        }).collect(Collectors.toList());

        // 尝试锁定库存
        for (SkuWareHasStock skuWareHasStock : collect) {
            Long skuId = skuWareHasStock.getSkuId();
            List<WareSkuEntity> wareSkuEntities = skuWareHasStock.getWareSkuEntities();
            if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
                // 当前商品没有库存了
                throw new NoStockExecption(skuId);
            }
            // 当前需要锁定的商品的梳理
            Integer count = skuWareHasStock.getNum();
            Boolean skuStocked = false; // 表示当前SkuId的库存没有锁定完成
            for (WareSkuEntity wareSkuEntity : wareSkuEntities) {
                // 循环获取到对应的 仓库，然后需要锁定库存
                // 获取当前仓库能够锁定的库存数
                Integer canStock = wareSkuEntity.getStock() - wareSkuEntity.getStockLocked();
                if (count <= canStock) {
                    // 表示当前的skuId的商品的数量小于等于需要锁定的数量
                    Integer integer = this.baseMapper.lockSkuStock(skuId, wareSkuEntity.getWareId(), count);
                    count = 0;
                    skuStocked = true;
                } else {
                    // 需要锁定的库存大于 可以锁定的库存 就按照已有的库存来锁定
                    Integer integer = this.baseMapper.lockSkuStock(skuId, wareSkuEntity.getWareId(), canStock);
                    count = count - canStock;
                }
                if (count <= 0) {
                    // 表示所有的商品都锁定了
                    break;
                }
            }
            if (count > 0) {
                // 说明库存没有锁定完
                throw new NoStockExecption(skuId);
            }
            if (skuStocked == false) {
                // 表示上一个商品的没有锁定库存成功
                throw new NoStockExecption(skuId);
            }
        }
        return true;
    }

    /**
     * 释放库存的操作
     *
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public Boolean orderReleaseStock(WareSkuLockVO vo) {
        List<OrderItemVo> items = vo.getItems();
        // 首先找到具有库存的仓库
        List<SkuWareHasStock> list = items.stream().map(item -> {
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            skuWareHasStock.setSkuId(item.getSkuId());
            List<WareSkuEntity> wareSkuEntities = this.baseMapper.listLockedStock(item.getSkuId());
            skuWareHasStock.setWareSkuEntities(wareSkuEntities);
            skuWareHasStock.setNum(item.getCount());
            return skuWareHasStock;
        }).collect(Collectors.toList());

        // 释放库存
        for (SkuWareHasStock skuWareHasStock : list) {
            Long skuId = skuWareHasStock.getSkuId();
            // 当前需要释放的商品的数量
            Integer count = skuWareHasStock.getNum();
            List<WareSkuEntity> wareSkuEntities = skuWareHasStock.getWareSkuEntities();
            for (WareSkuEntity wareSkuEntity : wareSkuEntities) {
                if (count <= 0) {
                    // 表示所有的商品都释放了
                    break;
                }
                Integer stockLocked = wareSkuEntity.getStockLocked();
                if (stockLocked >= count) {
                    // 表示当前需要释放的库存数量小于等于锁定的库存数量
                    Integer integer = this.baseMapper.releaseSkuStock(skuId, wareSkuEntity.getWareId(), count);
                    count = 0;
                } else {
                    // 需要释放的库存数量大于当前锁定的库存数量
                    Integer integer = this.baseMapper.releaseSkuStock(skuId, wareSkuEntity.getWareId(), stockLocked);
                    count = count - stockLocked;
                }
            }
        }
        return true;
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<WareSkuEntity> wareSkuEntities;
    }

}