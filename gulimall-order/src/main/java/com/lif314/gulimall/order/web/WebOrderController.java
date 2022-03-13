package com.lif314.gulimall.order.web;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebOrderController {

    @GetMapping("/{page}.html")
    public String testPage(@PathVariable("page") String page) {
        return page;
    }

}
