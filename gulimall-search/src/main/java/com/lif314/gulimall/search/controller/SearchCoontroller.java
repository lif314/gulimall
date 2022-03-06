package com.lif314.gulimall.search.controller;

import com.lif314.gulimall.search.service.SearchService;
import com.lif314.gulimall.search.vo.SearchParam;
import com.lif314.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller  // 页面跳转
public class SearchCoontroller {

    @Autowired
    SearchService searchService;

    /**
     * SpringMVC 自动将页面提交过来的所有请求的查询参数封装成指定的对象
     * @param searchParam 查询参数条件
     * @return 返回查询结果
     */
    // http://search.gmall.com/search.html?catalog3Id=165
    @GetMapping("/search.html")  // 点击分类跳转到搜索页面
    public String  listPage(SearchParam searchParam) {

        SearchResult result = searchService.search(searchParam);

        return "search";
    }



}
