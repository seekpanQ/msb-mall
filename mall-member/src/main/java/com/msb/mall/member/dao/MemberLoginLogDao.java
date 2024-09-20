package com.msb.mall.member.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.member.entity.MemberLoginLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员登录记录
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-20 16:38:34
 */
@Mapper
public interface MemberLoginLogDao extends BaseMapper<MemberLoginLogEntity> {

}
