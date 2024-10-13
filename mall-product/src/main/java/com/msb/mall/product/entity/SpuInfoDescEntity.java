package com.msb.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * spu信息介绍
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-19 22:59:19
 */
@Data
@TableName("pms_spu_info_desc")
public class SpuInfoDescEntity implements Serializable {
    private static final long serialVersionUID = 1L;

	/**
	 * 商品id
	 * 如果这个主键不是自增的，那么我们就需要设置这个字段的类型为
	 * IdType.INPUT
	 */
    @TableId(type = IdType.INPUT)
    private Long spuId;
    /**
     * 商品介绍
     */
    private String decript;

}
