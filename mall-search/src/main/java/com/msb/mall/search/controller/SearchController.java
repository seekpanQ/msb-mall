package com.msb.mall.search.controller;

import com.msb.mall.search.service.MallSearchService;
import com.msb.mall.search.vo.SearchParam;
import com.msb.mall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {

    @Autowired
    private MallSearchService mallSearchService;

    /**
     * 检索处理
     *
     * @return
     */
    @GetMapping(value = {"/list.html", "/", "/index.html"})
    public String listPage(SearchParam param, Model model) {
        // 通过对应的Service根据传递过来的相关的信息去ES中检索对应的数据
        SearchResult searchResult = mallSearchService.search(param);
        model.addAttribute("result", searchResult);
        return "index";
    }
}
