package com.msb.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.utils.PageUtils;
import com.msb.mall.product.entity.AttrGroupEntity;
import com.msb.mall.product.vo.AttrGroupWithAttrsVo;
import com.msb.mall.product.vo.SpuItemGroupAttrVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-19 22:59:19
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);

    List<AttrGroupWithAttrsVo> getAttrgroupWithAttrsByCatelogId(Long catelogId);

    List<SpuItemGroupAttrVo> getAttrgroupWithSpuId(Long spuId, Long catelogId);
}

