package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.oms.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * description:
 *
 * @author Ice on 2021/3/31 in 0:21
 */
@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("confirm")
    public String confirm(Model model) {
        OrderConfirmVo confirmVo = orderService.confirm();
        model.addAttribute("confirmVo", confirmVo);
        return "trade";
    }

    @PostMapping("submit")
    @ResponseBody
    public ResponseVo<String> submit(@RequestBody OrderSubmitVo orderSubmitVo) {
        orderService.submit(orderSubmitVo);
        return ResponseVo.ok(orderSubmitVo.getOrderToken());
    }
}
