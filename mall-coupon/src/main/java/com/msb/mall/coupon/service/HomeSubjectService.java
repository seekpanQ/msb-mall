package com.msb.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.utils.PageUtils;
import com.msb.mall.coupon.entity.HomeSubjectEntity;

import java.util.Map;

/**
 * 
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-20 16:15:02
 */
public interface HomeSubjectService extends IService<HomeSubjectEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

