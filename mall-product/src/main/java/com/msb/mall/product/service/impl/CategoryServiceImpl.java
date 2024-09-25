package com.msb.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.mall.product.dao.CategoryDao;
import com.msb.mall.product.entity.CategoryEntity;
import com.msb.mall.product.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查询所有的类别数据，然后将数据封装为树形结构，便于前端使用
     *
     * @param params
     * @return
     */
    @Override
    public List<CategoryEntity> queryPageWithTree(Map<String, Object> params) {
        // 1.查询所有的商品分类信息
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        // 2.将商品分类信息拆解为树形结构【父子关系】
        // 第一步遍历出所有的大类  parent_cid = 0
        List<CategoryEntity> list = categoryEntities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                // 根据大类找到多有的小类  递归的方式实现
                .map(categoryEntity -> {
                    categoryEntity.setChildrens(getCategoryChildren(categoryEntity, categoryEntities));
                    return categoryEntity;
                }).sorted((entity1, entity2) -> {
                    return (entity1.getSort() == null ? 0 : entity1.getSort())
                            - (entity2.getSort() == null ? 0 : entity2.getSort());
                })
                .collect(Collectors.toList());
        return list;
    }

    /**
     * 查找该大类下的所有的小类  递归查找
     *
     * @param categoryEntity   某个大类
     * @param categoryEntities 所有的类别数据
     * @return
     */
    private List<CategoryEntity> getCategoryChildren(CategoryEntity categoryEntity
            , List<CategoryEntity> categoryEntities) {
        List<CategoryEntity> collect =
                categoryEntities.stream()
                        // 根据大类找到他的直属的小类
                        .filter(entity -> {
                            return categoryEntity.getCatId() == entity.getParentCid();
                        })
                        // 根据这个小类递归找到对应的小小类
                        .map(entity -> {
                            entity.setChildrens(getCategoryChildren(entity, categoryEntities));
                            return entity;
                        }).sorted((entity1, entity2) -> {
                            return (entity1.getSort() == null ? 0 : entity1.getSort())
                                    - (entity2.getSort() == null ? 0 : entity2.getSort());
                        })
                        .collect(Collectors.toList());
        return collect;
    }

}