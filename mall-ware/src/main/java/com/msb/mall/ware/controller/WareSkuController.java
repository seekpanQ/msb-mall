package com.msb.mall.ware.controller;

import com.msb.common.dto.SkuHasStockDto;
import com.msb.common.exception.BizCodeEnum;
import com.msb.common.exception.NoStockExecption;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.R;
import com.msb.mall.ware.entity.WareSkuEntity;
import com.msb.mall.ware.service.WareSkuService;
import com.msb.mall.ware.vo.WareSkuLockVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 商品库存
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-10-15 11:02:55
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 锁定库存
     *
     * @param vo
     * @return
     */
    @PostMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVO vo) {
        try {
            Boolean flag = wareSkuService.orderLockStock(vo);
        } catch (NoStockExecption e) {
            // 表示锁定库存失败
            return R.error(BizCodeEnum.NO_STOCK_EXCEPTION.getCode(), BizCodeEnum.NO_STOCK_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    /**
     * 释放锁定的库存
     *
     * @param vo
     * @return
     */
    @PostMapping("/release/order")
    public R orderReleaseStock(@RequestBody WareSkuLockVO vo) {
        try {
            Boolean flag = wareSkuService.orderReleaseStock(vo);
        } catch (NoStockExecption e) {
            // 表示释放库存失败
            return R.error(BizCodeEnum.RELEASE_STOCK_EXCEPTION.getCode(), BizCodeEnum.RELEASE_STOCK_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id) {
        WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 查询对应的skuId是否有库存
     *
     * @param skuIds
     * @return
     */

    @PostMapping("/hasStock")
    public List<SkuHasStockDto> getSkusHasStock(@RequestBody List<Long> skuIds) {
        List<SkuHasStockDto> list = wareSkuService.getSkusHasStock(skuIds);
        return list;
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids) {
        wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
