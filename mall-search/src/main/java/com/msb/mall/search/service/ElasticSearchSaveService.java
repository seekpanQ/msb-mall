package com.msb.mall.search.service;

import com.msb.common.dto.es.SkuESModel;

import java.io.IOException;
import java.util.List;

public interface ElasticSearchSaveService {
    Boolean productStatusUp(List<SkuESModel> skuESModels) throws IOException;
}
