package com.msb.mall.member.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msb.mall.member.entity.MemberLevelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-20 16:38:34
 */
@Mapper
public interface MemberLevelDao extends BaseMapper<MemberLevelEntity> {

    MemberLevelEntity queryMemberLevelDefault();

}
