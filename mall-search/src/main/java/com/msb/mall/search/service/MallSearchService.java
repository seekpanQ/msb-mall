package com.msb.mall.search.service;

import com.msb.mall.search.vo.SearchParam;
import com.msb.mall.search.vo.SearchResult;

public interface MallSearchService {

    SearchResult search(SearchParam searchParam);
}
