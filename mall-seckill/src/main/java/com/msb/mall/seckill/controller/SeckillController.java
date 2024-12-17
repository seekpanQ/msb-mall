package com.msb.mall.seckill.controller;

import com.alibaba.fastjson.JSON;
import com.msb.common.utils.R;
import com.msb.mall.seckill.dto.SeckillSkuRedisDto;
import com.msb.mall.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController {
    @Autowired
    private SeckillService seckillService;

    @GetMapping("/currentSeckillSessionSkus")
    @ResponseBody
    public R getCurrentSeckillSessionSkus() {
        List<SeckillSkuRedisDto> currentSeckillSkus = seckillService.getCurrentSeckillSkus();
        return R.ok().put("data", JSON.toJSONString(currentSeckillSkus));
    }


}
