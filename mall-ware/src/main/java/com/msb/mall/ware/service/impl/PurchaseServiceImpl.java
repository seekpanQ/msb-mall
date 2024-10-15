package com.msb.mall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.mall.ware.dao.PurchaseDao;
import com.msb.mall.ware.entity.PurchaseEntity;
import com.msb.mall.ware.service.PurchaseService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

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


}