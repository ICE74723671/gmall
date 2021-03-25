package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * description:
 *
 * @author Ice on 2021/3/23 in 21:30
 */
@Controller
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping("{skuId}.html")
    public String loadData(@PathVariable("skuId")Long skuId, Model model) throws Exception {
        ItemVo itemVo = this.itemService.load(skuId);
        model.addAttribute("itemVo", itemVo);
//        this.itemService.createHtml(itemVo);
        return "item";
    }
}

