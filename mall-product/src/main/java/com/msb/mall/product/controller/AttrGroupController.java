package com.msb.mall.product.controller;

import com.msb.common.utils.PageUtils;
import com.msb.common.utils.R;
import com.msb.mall.product.entity.AttrEntity;
import com.msb.mall.product.entity.AttrGroupEntity;
import com.msb.mall.product.service.AttrAttrgroupRelationService;
import com.msb.mall.product.service.AttrGroupService;
import com.msb.mall.product.service.AttrService;
import com.msb.mall.product.service.CategoryService;
import com.msb.mall.product.vo.AttrGroupRelationVO;
import com.msb.mall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 属性分组
 *
 * @author Lison
 * @email lixin_qiu@163.com
 * @date 2024-09-19 22:59:19
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private AttrService attrService;
    @Autowired
    private AttrAttrgroupRelationService relationService;

    /**
     * 列表
     */
    @RequestMapping("/list/{catelogId}")
//    @RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params,
                  @PathVariable("catelogId") Long catelogId) {
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params, catelogId);
        return R.ok().put("page", page);
    }

    @RequestMapping("/{catelogId}/withattr")
    public R getAttrgroupWithAttrs(@PathVariable("catelogId") Long catelogId) {
        //根据三级分类编号获取对应属性组和属性组的基本信息
        List<AttrGroupWithAttrsVo> list
                = attrGroupService.getAttrgroupWithAttrsByCatelogId(catelogId);
        return R.ok().put("data", list);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
//    @RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        //根据找到的属性组对应的分类id找到对应的[一级，二级，三级]数据
        Long catelogId = attrGroup.getCatelogId();
        Long[] paths = categoryService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(paths);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 查询属性组属性信息
     *
     * @param attrgroupId
     * @return
     */
    @GetMapping("/{attrgroupId}/attr/relation")
//    @RequiresPermissions("product:attrgroup:save")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        List<AttrEntity> list = attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data", list);
    }

    @GetMapping("/{attrgroupId}/noattr/relation")
//    @RequiresPermissions("product:attrgroup:save")
    public R attrNoRelation(@PathVariable("attrgroupId") Long attrgroupId,
                            @RequestParam Map<String, Object> params) {
        PageUtils pageUtils = attrService.getNoAttrRelation(params, attrgroupId);
        return R.ok().put("page", pageUtils);
    }

    @PostMapping("/attr/relation/delete")
    public R relationDelete(@RequestBody AttrGroupRelationVO[] vos) {
        attrService.deleteRelation(vos);
        return R.ok();
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
//    @RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    @RequestMapping("/attr/relation")
    public R saveBatch(@RequestBody List<AttrGroupRelationVO> vos) {
        relationService.saveBatch(vos);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
//    @RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
//    @RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
