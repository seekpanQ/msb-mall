package com.msb.mall.seckill.service;

import com.msb.mall.seckill.dto.SeckillSkuRedisDto;

import java.util.List;

public interface SeckillService {

    void uploadSeckillSku3Days();

    List<SeckillSkuRedisDto> getCurrentSeckillSkus();
}