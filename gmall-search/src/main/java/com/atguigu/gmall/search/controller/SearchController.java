package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.service.SearchService;
import com.atguigu.gmall.search.vo.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * description:
 *
 * @author Ice on 2021/3/15 in 21:15
 */
@RestController
@RequestMapping("search")
public class SearchController {

    @Autowired
    SearchService searchService;

    @GetMapping
    public ResponseVo<SearchResponseVo> search(SearchParamVo searchParamVo) {
        return ResponseVo.ok(searchService.search(searchParamVo));
    }
}
