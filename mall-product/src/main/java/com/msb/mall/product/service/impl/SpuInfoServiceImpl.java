package com.msb.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.dto.MemberPrice;
import com.msb.common.dto.SkuReductionDTO;
import com.msb.common.dto.SpuBoundsDTO;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;
import com.msb.common.utils.R;
import com.msb.mall.product.dao.SpuInfoDao;
import com.msb.mall.product.entity.*;
import com.msb.mall.product.feign.CouponFeignService;
import com.msb.mall.product.service.*;
import com.msb.mall.product.vo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private SpuImagesService spuImagesService;
    @Autowired
    private ProductAttrValueService productAttrValueService;
    @Autowired
    private AttrService attrService;
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private CouponFeignService couponFeignService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private BrandService brandService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveSpuInfo(SpuInfoVO spuInfoVO) {
        // 1.保存spu的基本信息 pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuInfoVO, spuInfoEntity);
        // 创建时间和更新时间需要我们手动的来同步
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.save(spuInfoEntity);

        // 2.保存spu的详情信息 pms_spu_info_desc
        List<String> decripts = spuInfoVO.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(spuInfoEntity.getId());
        descEntity.setDecript(String.join(",", decripts));
        spuInfoDescService.save(descEntity);

        // 3.保存图集信息 pms_spu_images
        List<String> images = spuInfoVO.getImages();
        // 将每一个图片信息封装成一个SpuImagesEntity实体对象
        List<SpuImagesEntity> imagesEntities = images.stream().map(item -> {
            SpuImagesEntity entity = new SpuImagesEntity();
            entity.setSpuId(spuInfoEntity.getId());
            entity.setImgUrl(item);
            return entity;
        }).collect(Collectors.toList());
        spuImagesService.saveBatch(imagesEntities);

        // 4.保存规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = spuInfoVO.getBaseAttrs();
        List<ProductAttrValueEntity> ProductAttrValueEntities = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setSpuId(spuInfoEntity.getId());
            valueEntity.setAttrId(attr.getAttrId());
            valueEntity.setAttrValue(attr.getAttrValues());
            AttrEntity attrEntity = attrService.getById(attr.getAttrId());
            valueEntity.setAttrName(attrEntity.getAttrName());
            valueEntity.setQuickShow(attr.getShowDesc());
            return valueEntity;

        }).collect(Collectors.toList());
        productAttrValueService.saveBatch(ProductAttrValueEntities);

        // 5.保存当前的spu对应的所有的sku信息
        List<Skus> skus = spuInfoVO.getSkus();
        if (skus != null && skus.size() > 0) {
            // 5.1 保存sku的基本信息 pms_sku_info
            skus.forEach(item -> {
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoVO.getCatalogId());
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSaleCount(0L);
                List<Images> images1 = item.getImages();
                String defaultImage = "";
                for (Images image2 : images1) {
                    if (image2.getDefaultImg() == 1) {
                        //表示是默认的图片
                        defaultImage = image2.getImgUrl();
                    }
                }
                skuInfoEntity.setSkuDefaultImg(defaultImage);
                skuInfoService.save(skuInfoEntity);

                // 5.2 保存sku的图片信息 pms_sku_image
                List<SkuImagesEntity> skuImagesEntities = images1.stream().map(img -> {
                    SkuImagesEntity entity = new SkuImagesEntity();
                    entity.setSkuId(skuInfoEntity.getSkuId());
                    entity.setImgUrl(img.getImgUrl());
                    entity.setDefaultImg(img.getDefaultImg());
                    return entity;
                }).filter(img -> {
                    return img.getDefaultImg() == 1;
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(skuImagesEntities);

                // 5.3 保存满减信息，折扣，会员价 mall_sms: sms_sku_ladder sms_full_reduction sms_member_price
                SkuReductionDTO dto = new SkuReductionDTO();
                BeanUtils.copyProperties(item, dto);
                dto.setSkuId(skuInfoEntity.getSkuId());
                // 设置会员价
                if (item.getMemberPrice() != null && item.getMemberPrice().size() > 0) {
                    List<com.msb.common.dto.MemberPrice> list = item.getMemberPrice().stream().map(memberPrice -> {
                        com.msb.common.dto.MemberPrice mDto = new MemberPrice();
                        BeanUtils.copyProperties(memberPrice, mDto);
                        return mDto;
                    }).collect(Collectors.toList());
                    dto.setMemberPrice(list);
                }
                R r = couponFeignService.saveFullReductionInfo(dto);
                if (r.getCode() != 0) {
                    log.error("调用Coupon服务处理满减、折扣，会员价操作失败...");
                }
                // 5.4 sku的销售属性信息 pms_sku_sale_attr_value
                List<Attr> attrs = item.getAttr();
                List<SkuSaleAttrValueEntity> saleAttrValueEntities = attrs.stream().map(sale -> {
                    SkuSaleAttrValueEntity entity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(sale, entity);
                    entity.setSkuId(skuInfoEntity.getSkuId());
                    return entity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(saleAttrValueEntities);
            });
        }

        // 6.保存spu的积分信息：mall-sms: sms_spu_bounds
        Bounds bounds = spuInfoVO.getBounds();
        SpuBoundsDTO spuBoundsDTO = new SpuBoundsDTO();
        BeanUtils.copyProperties(bounds, spuBoundsDTO);
        spuBoundsDTO.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundsDTO);
        if (r.getCode() != 0) {
            log.error("调用Coupon服务存储积分信息操作失败");
        }

    }

    /**
     * SPU信息检索
     * 分页查询
     * 分类 品牌 状态 关键字查询
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        // 设置对应的检索条件
        // 1. 关键字查询
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            // 需要添加关键字查询
            wrapper.and((w) -> {
                w.eq("id", key)
                        .or().like("spu_name", key)
                        .or().like("spu_description", key);

            });
        }
        // status
        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq("publish_status", status);
        }
        // catalogId
        String catalogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catalogId) && !"0".equalsIgnoreCase(catalogId)) {
            wrapper.eq("catalog_id", catalogId);
        }
        // brandId
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );
        // 根据查询到的分页信息，再查询出对应的类别名称和品牌名称
        List<SpuInfoVO> list = page.getRecords().stream().map(spu -> {
            Long catalogId1 = spu.getCatalogId();
            Long brandId1 = spu.getBrandId();
            CategoryEntity categoryEntity = categoryService.getById(catalogId1);
            BrandEntity brandEntity = brandService.getById(brandId1);
            SpuInfoVO vo = new SpuInfoVO();
            BeanUtils.copyProperties(spu, vo);
            vo.setCatalogName(categoryEntity.getName());
            vo.setBrandName(brandEntity.getName());
            return vo;
        }).collect(Collectors.toList());
        IPage<SpuInfoVO> iPage = new Page<SpuInfoVO>();
        iPage.setRecords(list);
        iPage.setPages(page.getPages());
        iPage.setCurrent(page.getCurrent());
        return new PageUtils(iPage);
    }

}