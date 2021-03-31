package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * description:
 *
 * @author Ice on 2021/3/29 in 16:39
 */
@Service
public class CartAsyncService {

    @Autowired
    private CartMapper cartMapper;

    @Async
    public void updateCart(String userId, Long skuId, Cart cart) {
//        int i = 1 / 0;
        cartMapper.update(cart, new QueryWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }

    @Async
    public void insertCart(String userId,Cart cart) {
        cartMapper.insert(cart);
    }

    @Async
    public void deleteCartByUserId(String userId) {
        cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));
    }

    @Async
    public void deleteCartByUserIdAndSkuId(String userId, Long skuId) {
        cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }
}
