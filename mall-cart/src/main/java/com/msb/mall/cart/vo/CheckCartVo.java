package com.msb.mall.cart.vo;

import lombok.Data;

@Data
public class CheckCartVo {
    // 商品的编号 SkuId
    private Long skuId;
    //是否被选中，0没选中，1选中了
    private String isChecked = "1";
}
