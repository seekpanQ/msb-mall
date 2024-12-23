package com.msb.mall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.constant.WareConstant;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.mall.ware.dao.PurchaseDao;
import com.msb.mall.ware.entity.PurchaseDetailEntity;
import com.msb.mall.ware.entity.PurchaseEntity;
import com.msb.mall.ware.service.PurchaseDetailService;
import com.msb.mall.ware.service.PurchaseService;
import com.msb.mall.ware.service.WareSkuService;
import com.msb.mall.ware.vo.MergeVO;
import com.msb.mall.ware.vo.PurchaseDoneVO;
import com.msb.mall.ware.vo.PurchaseItemDoneVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService purchaseDetailService;
    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        String status = (String) params.get("status");
        if (StringUtils.isNotEmpty(status)) {
            wrapper.eq("status", status);
        }
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            wrapper.eq("id", key).or()
                    .eq("assignee_id", key).or()
                    .eq("ware_id", key).or()
                    .like("assignee_name", key);
        }
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 查询采购单的状态为 新建 或者 已分配 的采购单信息
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        //添加查询条件
        wrapper.eq("status", 0).or().eq("status", 1);
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 完成采购需求的合单操作
     *
     * @param mergeVO
     * @return
     */
    @Transactional
    @Override
    public Integer merge(MergeVO mergeVO) {
        Long purchaseId = mergeVO.getPurchaseId();
        if (purchaseId == null) {
            // 新建采购单
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        // 判断采购单的状态 只能是新建或者已分配的才能合单 如果是已领取 就不能再合单了
        PurchaseEntity entity = this.getById(purchaseId);
        if (entity.getStatus() > WareConstant.PurchaseStatusEnum.RECEIVE.getCode()) {
            // 该菜单不能合单
            return -1;
        }

        // 整合菜单需求单
        List<Long> items = mergeVO.getItems();
        final long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> list = items.stream().map(i -> {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            // 更新每一条 需求单的 采购单编号
            detailEntity.setId(i);
            detailEntity.setPurchaseId(finalPurchaseId);
            detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGED.getCode());
            return detailEntity;
        }).filter(id -> {
            PurchaseDetailEntity detailEntity = purchaseDetailService.getById(id);
            if (detailEntity.getStatus() == WareConstant.PurchaseDetailStatusEnum.CREATED.getCode()
                    || detailEntity.getStatus() == WareConstant.PurchaseDetailStatusEnum.ASSIGED.getCode()) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(list)) {
            purchaseDetailService.updateBatchById(list);
        }

        // 更新对应的采购单的更新时间
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
        return 1;
    }

    /**
     * 领取采购单
     *
     * @param ids
     */
    @Transactional
    @Override
    public void received(List<Long> ids) {
        // 1.领取的采购单的状态只能是新建或者已分配的采购单 其他的是不能领取的

        List<PurchaseEntity> list
                = ids.stream().map(id -> {
            return this.getById(id);
        }).filter(item -> {
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode()
                    || item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGED.getCode()) {
                return true;
            }
            return false;
        }).map(item -> {
            // 设置更新时间
            item.setUpdateTime(new Date());
            // 更新采购单的状态为 已领取
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            return item;
        }).collect(Collectors.toList());

        // 2.更新采购单的状态为 已领取
        this.updateBatchById(list);

        // 3.更新采购项的状态为 正在采购
        for (Long id : ids) {
            // 根据采购单id 找到对应的采购项对象
            List<PurchaseDetailEntity> detailEntities
                    = purchaseDetailService.listDetailByPurchaseId(id);

            List<PurchaseDetailEntity> collect = detailEntities.stream().map(item -> {
                PurchaseDetailEntity entity = new PurchaseDetailEntity();
                entity.setId(item.getId());
                entity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return entity;
            }).collect(Collectors.toList());
            // 批量更新采购项
            purchaseDetailService.updateBatchById(collect);
        }

    }

    @Transactional
    @Override
    public void done(PurchaseDoneVO purchaseDoneVO) {
        // 获取采购单编号
        Long id = purchaseDoneVO.getId();

        // 2.改变采购项的状态
        Boolean flag = true; // 记录采购的状态 默认为 完成
        List<PurchaseItemDoneVO> items = purchaseDoneVO.getItems();
        List<PurchaseDetailEntity> list = new ArrayList<>();
        for (PurchaseItemDoneVO item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                // 该采购项采购出现了问题
                flag = false;
                detailEntity.setStatus(item.getStatus());
            } else {
                // 采购项采购成功
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                // 3.将采购成功的采购项进入库操作
                // 跟进采购项编号查询出对应的采购项详情
                PurchaseDetailEntity detailEntity1 = purchaseDetailService.getById(item.getItemId());

                wareSkuService.addStock(detailEntity1.getSkuId(),
                        detailEntity1.getWareId(), detailEntity1.getSkuNum());
                detailEntity.setId(item.getItemId());
                list.add(detailEntity);
            }
        }
        purchaseDetailService.updateBatchById(list);// 批量更新 采购项

        // 1.改变采购单的状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        purchaseEntity.setStatus(flag ? WareConstant.PurchaseStatusEnum.FINISH.getCode()
                : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

}