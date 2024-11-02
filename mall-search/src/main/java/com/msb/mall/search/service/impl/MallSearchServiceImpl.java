package com.msb.mall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.msb.common.dto.es.SkuESModel;
import com.msb.mall.search.config.MallElasticSearchConfiguration;
import com.msb.mall.search.constant.ESConstant;
import com.msb.mall.search.service.MallSearchService;
import com.msb.mall.search.vo.SearchParam;
import com.msb.mall.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private RestHighLevelClient client;

    @Override
    public SearchResult search(SearchParam searchParam) {
        SearchResult searchResult = null;
        // 1. 准备检索的请求
        SearchRequest request = buildSearchRequest(searchParam);
        try {
            // 2.执行检索操作
            SearchResponse response = client.search(request, MallElasticSearchConfiguration.COMMON_OPTIONS);
            // 3.需要把检索的信息封装为SearchResult
            searchResult = buildSearchResult(response, searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchResult;
    }

    /**
     * 构建检索的请求
     * 模糊匹配，关键字匹配
     * 过滤(类别，品牌，属性，价格区间，库存)
     * 排序
     * 分页
     * 高亮
     * 聚合分析
     *
     * @param param
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(ESConstant.PRODUCT_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 构建具体的检索的条件
        // 1.构建bool查询
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        // 1.1 关键字的条件
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            String keyword = param.getKeyword();//关键字去重
            if (keyword.contains(",")) {
                String[] keywordArr = param.getKeyword().split(",");
                keyword = keywordArr[keywordArr.length - 1];
            }
            boolQuery.must(QueryBuilders.matchQuery("subTitle", keyword));
        }
        // 1.2 类别的检索条件
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // 1.3 品牌的检索条件
        if (!CollectionUtils.isEmpty(param.getBrandId())) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // 1.4 是否有库存
        if (param.getHasStock() != null) {
            boolQuery.filter(QueryBuilders.termsQuery("hasStock", param.getHasStock() == 1));
        }
        // 1.5 根据价格区间来检索
        if (StringUtils.isNotEmpty(param.getSkuPrice())) {
            String[] msg = param.getSkuPrice().split("_");
            RangeQueryBuilder skuPrice = QueryBuilders.rangeQuery("skuPrice");
            if (msg.length == 2) {
                // 说明是 200_300
                skuPrice.gte(msg[0]);
                skuPrice.lte(msg[1]);
            } else if (msg.length == 1) {
                // 说明是 _300  200_
                if (param.getSkuPrice().endsWith("_")) {
                    // 说明是 200_
                    skuPrice.gte(msg[0]);
                }
                if (param.getSkuPrice().startsWith("_")) {
                    // 说明是 _300
                    skuPrice.lte(msg[0]);
                }
            }
            boolQuery.filter(skuPrice);
        }
        // 1.6 属性的检索条件 attrs=20_8英寸:10英寸&attrs=19_64GB:32GB
        if (!CollectionUtils.isEmpty(param.getAttrs())) {
            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder boolNestedQuery = QueryBuilders.boolQuery();
                // attrs=19_64GB:32GB 我们首先需要根据 _ 做分割
                String[] attrStrArr = attrStr.split("_");
                // 属性的编号
                String attrId = attrStrArr[0];
                // 64GB:32GB  获取属性的值
                String[] values = attrStrArr[1].split(":");
                // 拼接组合条件
                boolNestedQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                boolNestedQuery.must(QueryBuilders.termsQuery("attrs.attrValue", values));
                NestedQueryBuilder nestedQuery =
                        QueryBuilders.nestedQuery("attrs", boolNestedQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
        sourceBuilder.query(boolQuery);

        // 2.排序
        if (StringUtils.isNotEmpty(param.getSort())) {
            // sort=salaCount_asc/desc
            String[] s = param.getSort().split("_");
            SortOrder sortOrder = "asc".equalsIgnoreCase(s[1]) ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], sortOrder);
        }
        // 3.处理分页
        // Integer pageNum; // 页码
        if (param.getPageNum() != null) {
            // 需要做分页处理 pageSize = 5
            // pageNum:1 from:0  [0,1,2,3,4]
            // pageNum:2 from:5 [5,6,7,8,9]
            // from = ( pageNum - 1 ) * pageSize
            sourceBuilder.from((param.getPageNum() - 1) * ESConstant.PRODUCT_PAGESIZE);
            sourceBuilder.size(ESConstant.PRODUCT_PAGESIZE);
        }
        // 4. 设置高亮
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("subTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }

        // 5.聚合运算
        // 5.1 品牌的聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg");
        brandAgg.field("brandId");
        brandAgg.size(50);
        // 品牌的子聚合
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(10));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(10));
        sourceBuilder.aggregation(brandAgg);

        // 5.2 类别的聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg");
        catalogAgg.field("catalogId");
        catalogAgg.size(10);
        // 类别的子聚合
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(10));
        sourceBuilder.aggregation(catalogAgg);

        // 5.3 属性的聚合
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
        // 属性id聚合
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg");
        attrIdAgg.field("attrs.attrId");
        attrIdAgg.size(10);
        // 属性id下的子聚合 属性名称和属性值
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(10));
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(10));
        attrAgg.subAggregation(attrIdAgg);
        sourceBuilder.aggregation(attrAgg);

        System.out.println("--->" + sourceBuilder.toString());
        searchRequest.source(sourceBuilder);

        return searchRequest;
    }

    /**
     * 根据检索的结果解析封装为SearchResult对象
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();
        SearchHits hits = response.getHits();
        // 1.检索的所有商品信息
        SearchHit[] products = hits.getHits();
        List<SkuESModel> esModels = new ArrayList<>();
        if (products != null && products.length > 0) {
            for (SearchHit product : products) {
                String sourceAsString = product.getSourceAsString();
                // 把json格式的字符串通过fastjson转换为SkuESModel对象
                SkuESModel model = JSON.parseObject(sourceAsString, SkuESModel.class);
                if (StringUtils.isNotEmpty(param.getKeyword())) {
                    // 我们需要设置高亮
                    HighlightField subTitle = product.getHighlightFields().get("subTitle");
                    String subTitleHighlight = subTitle.getFragments()[0].toString();
                    model.setSubTitle(subTitleHighlight);//设置高亮
                }
                esModels.add(model);
            }
        }
        result.setProducts(esModels);
        Aggregations aggregations = response.getAggregations();

        // 2.当前商品所涉及到的所有的品牌
        ParsedLongTerms brandAgg = aggregations.get("brand_agg");
        List<? extends Terms.Bucket> buckets = brandAgg.getBuckets();
        // 存储所有品牌的容器
        List<SearchResult.BrandVO> brandVOS = new ArrayList<>();
        if (buckets != null && buckets.size() > 0) {
            for (Terms.Bucket bucket : buckets) {
                SearchResult.BrandVO brandVO = new SearchResult.BrandVO();
                String keyAsString = bucket.getKeyAsString();
                brandVO.setBrandId(Long.parseLong(keyAsString));// 设置品牌的编号
                // 然后我们需要获取品牌的名称和图片的地址
                ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
                List<? extends Terms.Bucket> bucketsImg = brandImgAgg.getBuckets();
                if (bucketsImg != null && bucketsImg.size() > 0) {
                    String img = bucketsImg.get(0).getKeyAsString();
                    brandVO.setBrandImg(img);
                }
                // 获取品牌名称的信息
                ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
                String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
                brandVO.setBrandName(brandName);
                brandVOS.add(brandVO);
            }
        }
        result.setBrands(brandVOS);

        // 3.当前商品涉及到的所有的类别信息
        ParsedLongTerms catalogAgg = aggregations.get("catalog_agg");
        List<? extends Terms.Bucket> bucketsCatalogs = catalogAgg.getBuckets();
        // 创建一个保存所有类别的容器
        List<SearchResult.CatalogVO> catalogVOS = new ArrayList<>();
        if (bucketsCatalogs != null && bucketsCatalogs.size() > 0) {
            for (Terms.Bucket bucket : bucketsCatalogs) {
                SearchResult.CatalogVO catalogVO = new SearchResult.CatalogVO();
                String keyAsString = bucket.getKeyAsString();// 获取类别的编号
                catalogVO.setCatalogId(Long.parseLong(keyAsString));
                // 获取类别的名称
                ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
                String catalogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
                catalogVO.setCatalogName(catalogName);
                catalogVOS.add(catalogVO);
            }

        }
        result.setCatalogs(catalogVOS);

        // 4.当前商品涉及到的所有的属性信息
        ParsedNested attrAgg = aggregations.get("attr_agg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
        List<? extends Terms.Bucket> bucketsAttr = attrIdAgg.getBuckets();
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        if (bucketsAttr != null && bucketsAttr.size() > 0) {
            for (Terms.Bucket bucket : bucketsAttr) {
                SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
                // 获取属性的编号
                String keyAsString = bucket.getKeyAsString();
                attrVo.setAttrId(Long.parseLong(keyAsString));
                // 又得分别获取 属性的名称 和 属性的值
                ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
                String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();// 属性的名称
                attrVo.setAttrName(attrName);
                ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
                if (attrValueAgg.getBuckets() != null && attrValueAgg.getBuckets().size() > 0) {
                    List<String> values = attrValueAgg.getBuckets().stream().map(item -> {
                        String keyAsString1 = item.getKeyAsString();
                        return keyAsString1;
                    }).collect(Collectors.toList());
                    attrVo.setAttrValue(values);
                }
                attrVos.add(attrVo);
            }
        }
        result.setAttrs(attrVos);
        // 5. 分页信息  当前页 总的记录数  总页数
        long total = hits.getTotalHits().value;
        result.setTotal(total);// 设置总记录数  6 /5  1+1
        result.setPageNum(param.getPageNum());// 设置当前页
        long totalPage = total % ESConstant.PRODUCT_PAGESIZE == 0 ?
                total / ESConstant.PRODUCT_PAGESIZE : (total / ESConstant.PRODUCT_PAGESIZE + 1);
        // 设置总的页数
        result.setTotalPages((int) totalPage);
        List<Integer> navs = new ArrayList<>();
        for (int i = 1; i <= totalPage; i++) {
            navs.add(i);
        }
        result.setNavs(navs);
        return result;
    }


}
