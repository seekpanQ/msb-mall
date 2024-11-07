package com.msb.mall.product.web;

import com.msb.mall.product.service.SkuInfoService;
import com.msb.mall.product.vo.SpuItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品详情的控制器
 */
@Controller
public class ItemController {

    @Autowired
    private SkuInfoService skuInfoService;

    /**
     * 根据前端传递的SkuId我们需要查询出对有的商品信息
     *
     * @param skuId
     * @return
     */
    @GetMapping("/{skuId}.html")
    public String item(@PathVariable("skuId") Long skuId, Model model) {
        SpuItemVO itemVO = skuInfoService.item(skuId);
        model.addAttribute("item", itemVO);
        return "item";
    }
}
