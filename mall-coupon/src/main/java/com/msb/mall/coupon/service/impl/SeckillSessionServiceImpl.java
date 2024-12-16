package com.msb.mall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.mall.coupon.dao.SeckillSessionDao;
import com.msb.mall.coupon.entity.SeckillSessionEntity;
import com.msb.mall.coupon.entity.SeckillSkuRelationEntity;
import com.msb.mall.coupon.service.SeckillSessionService;
import com.msb.mall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {
    @Autowired
    private SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLates3DaysSession() {
        // 计算未来3天的时间
        List<SeckillSessionEntity> list = this.list(new QueryWrapper<SeckillSessionEntity>()
                .between("start_time", startTime(), endTime()));
        List<SeckillSessionEntity> seckillSessionEntities = list.stream().map(session -> {
            // 根据对应的sessionId活动编号查询出对应的活动商品信息
            List<SeckillSkuRelationEntity> relationEntities = seckillSkuRelationService.list(
                    new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", session.getId()));
            session.setRelationEntities(relationEntities);
            return session;
        }).collect(Collectors.toList());
        return seckillSessionEntities;
    }

    private String startTime() {
        LocalDate now = LocalDate.now();
        LocalDate startDay = now.plusDays(0);
        LocalDateTime start = LocalDateTime.of(startDay, LocalTime.MIN);
        return start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String endTime() {
        LocalDate now = LocalDate.now();
        LocalDate startDay = now.plusDays(2);
        LocalDateTime start = LocalDateTime.of(startDay, LocalTime.MAX);
        return start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}