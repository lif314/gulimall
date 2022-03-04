package com.lif314.gulimall.product.web;

import com.lif314.gulimall.product.entity.CategoryEntity;
import com.lif314.gulimall.product.service.CategoryService;
import com.lif314.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    /**
     * 更改访问路由
     * / /index.html 都跳转到首页
     * model: 页面
     * @return 页面地址
     */
    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model){

        // 查出所有的一级分类
        List<CategoryEntity> categoryEntityList =  categoryService.getLevel1Category();
        // 将数据放在页面中
        model.addAttribute("categories", categoryEntityList);
        /**
         * 返回逻辑视图。视图解析器进行拼串
         * 默认前缀：classpath:/templates
         * 默认后缀：.html
         * 实际路由：classpath:/templates/index.html
         */
        return "index";
    }


    // index/catalog.json
    @ResponseBody // 返回数据，而不是跳转页面
    @GetMapping("index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson(){
        Map<String, List<Catelog2Vo>> map =  categoryService.getCatelogJson();
        return map;
    }


}
