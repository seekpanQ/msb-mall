package com.msb.mall.search.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {

    /**
     * 检索处理
     *
     * @return
     */
    @GetMapping(value = {"/list.html", "/", "/index.html"})
    public String listPage(Model model) {

        return "index";
    }
}
