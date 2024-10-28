package com.msb.mall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.mall.product.dao.CategoryDao;
import com.msb.mall.product.entity.CategoryEntity;
import com.msb.mall.product.service.CategoryBrandRelationService;
import com.msb.mall.product.service.CategoryService;
import com.msb.mall.product.vo.Catalog2VO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

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
                    categoryEntity.setChildren(getCategoryChildren(categoryEntity, categoryEntities));
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
                            //注意 Long数据比较，不在-128~127之间的数据是new Long()对象
                            return entity.getParentCid().equals(categoryEntity.getCatId());
                        })
                        // 根据这个小类递归找到对应的小小类
                        .map(entity -> {
                            entity.setChildren(getCategoryChildren(entity, categoryEntities));
                            return entity;
                        }).sorted((entity1, entity2) -> {
                            return (entity1.getSort() == null ? 0 : entity1.getSort())
                                    - (entity2.getSort() == null ? 0 : entity2.getSort());
                        })
                        .collect(Collectors.toList());
        return collect;
    }

    /**
     * 逻辑批量删除操作
     *
     * @param ids
     */
    @Override
    public void removeCategoryByIds(List<Long> ids) {
        // TODO  1.检查类别数据是否在其他业务中使用

        // 2.批量逻辑删除操作
        baseMapper.deleteBatchIds(ids);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    @Transactional
    @Override
    public void updateDetail(CategoryEntity category) {
        this.updateById(category);

        categoryBrandRelationService.updateCatelogName(category.getCatId(), category.getName());
    }

    /**
     * 查询出所有的商品大类(一级分类)
     *
     * @return
     */
    @Override
    public List<CategoryEntity> getLeve1Category() {
        List<CategoryEntity> list = baseMapper.queryLeve1Category();
        return list;
    }

    /**
     * 跟进父编号获取对应的子菜单信息
     *
     * @param list
     * @param parentCid
     * @return
     */
    private List<CategoryEntity> queryByParenCid(List<CategoryEntity> list, Long parentCid) {
        List<CategoryEntity> collect = list.stream().filter(item -> {
            return item.getParentCid().equals(parentCid);
        }).collect(Collectors.toList());
        return collect;
    }


    public Map<String, List<Catalog2VO>> getCatelog2JSON() {
        String keys = "catalogJSON";
        // 加锁,在执行插入操作的同时设置了过期时间,保证原子性操作
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue()
                .setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            // 加锁成功
            Map<String, List<Catalog2VO>> data = null;
            try {
                data = getDataForDB(keys);
            } finally {
                String srcipts = "if redis.call('get',KEYS[1])==ARGV[1] " +
                        "then return redis.call('del',KEYS[1]) else return 0 end";
                // 通过Redis的lua脚本实现 查询和删除操作的原子性
                stringRedisTemplate.execute(new DefaultRedisScript<Long>(srcipts, Long.class),
                        Arrays.asList("lock"), uuid);
            }
            return data;
        } else {
            // 加锁失败
            // 休眠+重试
//             Thread.sleep(1000);
            return getCatelog2JSON();
        }
    }

    /**
     * 从数据库中查询操作
     *
     * @param keys
     * @return
     */
    private Map<String, List<Catalog2VO>> getDataForDB(String keys) {
        // 从Redis中获取分类的信息
        String catalogJSON = stringRedisTemplate.opsForValue().get(keys);
        if (!org.springframework.util.StringUtils.isEmpty(catalogJSON)) {
            // 说明缓存命中
            // 表示缓存命中了数据，那么从缓存中获取信息，然后返回
            Map<String, List<Catalog2VO>> stringListMap = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2VO>>>() {
            });
            return stringListMap;
        }
        System.out.println("-----------》查询数据库操作");

        // 获取所有的分类数据
        List<CategoryEntity> list = baseMapper.selectList(new QueryWrapper<CategoryEntity>());
        // 获取所有的一级分类的数据
        List<CategoryEntity> leve1Category = this.queryByParenCid(list, 0l);
        // 把一级分类的数据转换为Map容器 key就是一级分类的编号， value就是一级分类对应的二级分类的数据
        Map<String, List<Catalog2VO>> map = leve1Category.stream().collect(Collectors.toMap(
                key -> key.getCatId().toString()
                , value -> {
                    // 根据一级分类的编号，查询出对应的二级分类的数据
                    List<CategoryEntity> l2Catalogs = this.queryByParenCid(list, value.getCatId());
                    List<Catalog2VO> Catalog2VOs = null;
                    if (l2Catalogs != null) {
                        Catalog2VOs = l2Catalogs.stream().map(l2 -> {
                            // 需要把查询出来的二级分类的数据填充到对应的Catelog2VO中
                            Catalog2VO catalog2VO = new Catalog2VO(l2.getParentCid().toString(), null, l2.getCatId().toString(), l2.getName());
                            // 根据二级分类的数据找到对应的三级分类的信息
                            List<CategoryEntity> l3Catelogs = this.queryByParenCid(list, l2.getCatId());
                            if (l3Catelogs != null) {
                                // 获取到的二级分类对应的三级分类的数据
                                List<Catalog2VO.Catalog3VO> catalog3VOS = l3Catelogs.stream().map(l3 -> {
                                    Catalog2VO.Catalog3VO catalog3VO = new Catalog2VO.Catalog3VO(l3.getParentCid().toString(), l3.getCatId().toString(), l3.getName());
                                    return catalog3VO;
                                }).collect(Collectors.toList());
                                // 三级分类关联二级分类
                                catalog2VO.setCatalog3List(catalog3VOS);
                            }
                            return catalog2VO;
                        }).collect(Collectors.toList());
                    }

                    return Catalog2VOs;
                }
        ));
        // 从数据库中获取到了对应的信息 然后在缓存中也存储一份信息
        //cache.put("getCatelog2JSON",map);
        // 表示缓存命中了数据，那么从缓存中获取信息，然后返回
        if (map == null) {
            // 那就说明数据库中也不存在  防止缓存穿透
            stringRedisTemplate.opsForValue().set(keys, "1", 5, TimeUnit.SECONDS);
        } else {
            // 从数据库中查询到的数据，我们需要给缓存中也存储一份
            // 防止缓存雪崩
            String json = JSON.toJSONString(map);
            stringRedisTemplate.opsForValue().set("catalogJSON", json, 100, TimeUnit.MINUTES);
        }
        return map;
    }

    /**
     * 查询出所有的二级和三级分类的数据
     * 并封装为Map<String, Catalog2VO>对象
     *
     * @return
     */
    public Map<String, List<Catalog2VO>> getCatelog2JSON11() {
        // 从Redis中获取分类的信息
        String key = "catalogJSON";
        String catalogJSON = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(catalogJSON)) {
            // 缓存中没有数据，需要从数据库中查询
            System.out.println("缓存没有命中.....");
            Map<String, List<Catalog2VO>> catelog2JSONForDb = getCatelog2JSONForDb();
            if (catelog2JSONForDb == null) {
                //说明数据不存在，放入空值结果缓存，并设置过期时间，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "1", 5, TimeUnit.SECONDS);
            }
            // 从数据库中查询到的数据，我们需要给缓存中也存储一份
            //设置随机过期时间，防止缓存雪崩
            String json = JSON.toJSONString(catelog2JSONForDb);
            stringRedisTemplate.opsForValue().set(key, json, new Random().nextInt(10) + 1, TimeUnit.HOURS);
            return catelog2JSONForDb;
        }
        System.out.println("缓存命中了....");
        // 表示缓存命中了数据，那么从缓存中获取信息，然后返回
        Map<String, List<Catalog2VO>> stringListMap
                = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2VO>>>() {
        });
        return stringListMap;
    }

    public Map<String, List<Catalog2VO>> getCatelog2JSONForDb() {
        String keys = "catalogJSON";
        synchronized (this) {//加同步锁，防止缓存击穿
            String catalogJSON = stringRedisTemplate.opsForValue().get(keys);
            if (StringUtils.isNotEmpty(catalogJSON)) {
                // 说明缓存命中
                // 表示缓存命中了数据，那么从缓存中获取信息，然后返回
                Map<String, List<Catalog2VO>> stringListMap =
                        JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2VO>>>() {
                        });
                return stringListMap;
            }
            System.out.println("-----------》查询数据库操作");
            // 获取所有的分类数据
            List<CategoryEntity> list = baseMapper.selectList(new QueryWrapper<CategoryEntity>());

            // 获取所有的一级分类的数据
            List<CategoryEntity> leve1Category = queryByParenCid(list, 0l);
            // 把一级分类的数据转换为Map容器 key就是一级分类的编号， value就是一级分类对应的二级分类的数据
            Map<String, List<Catalog2VO>> map =
                    leve1Category.stream().collect(Collectors.toMap(key -> key.getCatId().toString(), value -> {
                        // 根据一级分类的编号，查询出对应的二级分类的数据
                        List<CategoryEntity> l2Catalogs = this.queryByParenCid(list, value.getCatId());
                        List<Catalog2VO> Catalog2VOs = null;
                        if (l2Catalogs != null) {
                            // 需要把查询出来的二级分类的数据填充到对应的Catelog2VO中
                            Catalog2VOs = l2Catalogs.stream().map(l2 -> {
                                Catalog2VO catalog2VO = new Catalog2VO(l2.getParentCid().toString(),
                                        null, l2.getCatId().toString(), l2.getName());
                                // 根据二级分类的数据找到对应的三级分类的信息
                                List<CategoryEntity> l3Catelogs = this.queryByParenCid(list, l2.getCatId());
                                if (l3Catelogs != null) {
                                    // 获取到的二级分类对应的三级分类的数据
                                    List<Catalog2VO.Catalog3VO> catalog3VOS = l3Catelogs.stream().map(l3 -> {
                                        Catalog2VO.Catalog3VO catalog3VO = new Catalog2VO.Catalog3VO(
                                                l3.getParentCid().toString(), l3.getCatId().toString(), l3.getName());
                                        return catalog3VO;
                                    }).collect(Collectors.toList());
                                    // 三级分类关联二级分类
                                    catalog2VO.setCatalog3List(catalog3VOS);
                                }
                                return catalog2VO;
                            }).collect(Collectors.toList());
                        }

                        return Catalog2VOs;
                    }));
            // 从数据库中获取到了对应的信息 然后在缓存中也存储一份信息
            if (map == null) {
                // 那就说明数据库中也不存在  防止缓存穿透
                stringRedisTemplate.opsForValue().set(keys, "1", 5, TimeUnit.SECONDS);
            } else {
                // 从数据库中查询到的数据，我们需要给缓存中也存储一份
                // 防止缓存雪崩
                String json = JSON.toJSONString(map);
                stringRedisTemplate.opsForValue().set("catalogJSON", json, new Random().nextInt(10) + 1, TimeUnit.MINUTES);
            }
            return map;
        }
    }

    /**
     * 递归实现找出分类层级
     *
     * @param catelogId
     * @param paths
     * @return
     */
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity categoryEntity = this.getById(catelogId);
        if (categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), paths);
        }
        return paths;
    }

}