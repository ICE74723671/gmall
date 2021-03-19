package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import io.swagger.annotations.ApiOperation;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/19 in 8:21
 */
@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @ApiOperation("跳转首页")
    @GetMapping
    public String toIndex(Model model) {
        List<CategoryEntity> categoryEntityList = indexService.queryLv1Categories();
        model.addAttribute("categories", categoryEntityList);
        return "index";
    }

    @ApiOperation("查询一级目录下的二级目录及三级目录")
    @GetMapping("/index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLv2WithSubs(@PathVariable("pid") Long pid) {
        List<CategoryEntity> entityList = indexService.queryLv2WithSubs(pid);
        return ResponseVo.ok(entityList);
    }
}