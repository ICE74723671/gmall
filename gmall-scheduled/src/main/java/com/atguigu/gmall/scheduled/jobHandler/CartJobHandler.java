package com.atguigu.gmall.scheduled.jobHandler;

import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.atguigu.gmall.scheduled.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import sun.nio.cs.US_ASCII;

import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/30 in 21:08
 */
@Component
public class CartJobHandler {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EXCEPTION_KEY = "cart:exception";

    private static final String KEY_PREFIX = "cart:info:";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @XxlJob("cartExceptionHandler")
    public ReturnT<String> exceptionHandler(String param) {
        BoundSetOperations<String, String> setOps = redisTemplate.boundSetOps(EXCEPTION_KEY);
        String userId = setOps.pop();

        while (StringUtils.isNotBlank(userId)) {
            //先删除mysql中的记录
            cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId));
            //读取redis中的记录
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartsJson = hashOps.values();
            //新增到mysql中
            if (!CollectionUtils.isEmpty(cartsJson)) {
                cartsJson.forEach(cart -> {
                    try {
                        Cart cart1 = MAPPER.readValue(cart.toString(), Cart.class);
                        cartMapper.insert(cart1);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
            }

            userId = setOps.pop();
        }

        return ReturnT.SUCCESS;
    }
}
