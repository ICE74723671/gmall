package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/28 in 10:23
 */
@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("check/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCarts(@PathVariable("userId") Long userId) {
        List<Cart> carts = cartService.queryCheckedCarts(userId);
        return ResponseVo.ok(carts);
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam("skuId") Long skuId) {
        cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart) {
        cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("updateStatus")
    @ResponseBody
    public ResponseVo updateStatus(@RequestBody Cart cart) {
        cartService.updateStatus(cart);
        return ResponseVo.ok();
    }

    @GetMapping("cart.html")
    public String queryCarts(Model model) {
        List<Cart> carts = cartService.queryCarts();
        model.addAttribute("carts", carts);
        return "cart";
    }

    @GetMapping
    public String addCart(Cart cart) {
        if (cart == null || cart.getSkuId() == null) {
            throw new RuntimeException("没有选择添加到购物车的信息！");
        }
        cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId() + "&count=" + cart.getCount();
    }

    @GetMapping("addCart.html")
    public String queryCartBySkuId(@RequestParam("skuId") Long skuId, @RequestParam("count") Integer count, Model model) {
        Cart cart = cartService.queryCartBySkuId(skuId, count);
        model.addAttribute("cart", cart);
        return "addCart";
    }

    @GetMapping("test")
    @ResponseBody
    public String test() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        System.out.println(userInfo);
        return "hello!!!!";
    }
}
