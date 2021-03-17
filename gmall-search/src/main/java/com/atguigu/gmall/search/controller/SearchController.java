package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.service.SearchService;
import com.atguigu.gmall.search.vo.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * description:
 *
 * @author Ice on 2021/3/15 in 21:15
 */
@Controller
@RequestMapping("search")
public class SearchController {

    @Autowired
    SearchService searchService;

    @GetMapping
    public String search(SearchParamVo searchParamVo, Model model) {
        SearchResponseVo responseVo = searchService.search(searchParamVo);
        model.addAttribute("response", responseVo);
        model.addAttribute("searchParam", searchParamVo);
        return "search";
    }
}
