package com.msb.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.mall.product.dao.AttrAttrgroupRelationDao;
import com.msb.mall.product.dao.AttrDao;
import com.msb.mall.product.entity.AttrAttrgroupRelationEntity;
import com.msb.mall.product.entity.AttrEntity;
import com.msb.mall.product.service.AttrService;
import com.msb.mall.product.vo.AttrGroupRelationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 根据属性组编号查询对应的基本信息
     *
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        // 1. 根据属性组编号从 属性组和基本信息的关联表中查询出对应的属性信息
        List<AttrAttrgroupRelationEntity> list
                = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .eq("attr_group_id", attrgroupId));
        // 2.根据属性id数组获取对应的详情信息
        List<AttrEntity> attrEntities = list.stream()
                .map((entity) -> this.getById(entity.getAttrId()))
                .filter((entity) -> entity != null).collect(Collectors.toList());
        return attrEntities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVO[] vos) {
        //讲接收的数据对象转为一个entity实体对象
        List<AttrAttrgroupRelationEntity> list = Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity entity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, entity);
            return entity;
        }).collect(Collectors.toList());
        //批量删除关联表中的数据
        attrAttrgroupRelationDao.removeBatchRelation(list);
    }

}