package com.msb.mall.search.controller;

import com.msb.common.dto.es.SkuESModel;
import com.msb.common.exception.BizCodeEnum;
import com.msb.common.utils.R;
import com.msb.mall.search.service.ElasticSearchSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 存储商城数据到ElasticSearch的服务
 */
@Slf4j
@RequestMapping("/search/save")
@RestController
public class ElasticSearchSaveController {
    @Autowired
    private ElasticSearchSaveService service;

    /**
     * 存储商品上架信息到ElasticSearch服务的接口
     *
     * @return
     */
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuESModel> skuESModels) {
        Boolean b = false;
        try {
            b = service.productStatusUp(skuESModels);
        } catch (IOException e) {
            log.error("ElasticSearch商品上架错误：{}", e);
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }
        if (b) {
            return R.ok();
        }
        return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
    }
}
