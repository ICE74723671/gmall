package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import springfox.documentation.spring.web.json.Json;
import sun.awt.geom.AreaOp;
import sun.nio.cs.US_ASCII;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * description:
 *
 * @author Ice on 2021/3/28 in 10:24
 */
@Service
public class CartService {

    @Autowired
    private CartAsyncService cartAsyncService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;

    private static final String KEY_PREFIX = "cart:info:";

    public void addCart(Cart cart) {
        //1.获取登录状态；已登录—userId，未登录-userKey
        String userId = getUserId();
        String key = KEY_PREFIX + userId;

        //2.判断当前用户的购物车中是否含有该记录
        //内层Map<skuId,cart>
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        String skuIdString = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuIdString)) {
            //包含，更新数量
            String json = hashOps.get(skuIdString).toString();
            cart = JSON.parseObject(json, Cart.class);
            //数量增加
            cart.setCount(cart.getCount().add(count));
            //把更新后的cart写入mysql和redis
            cartAsyncService.updateCart(userId, cart.getSkuId(), cart);
        } else {
            //不包含，增加一条记录
            cart.setCheck(true);
            cart.setUserId(userId);
            //查询sku信息
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new CartException("加入购物车的商品不存在!");
            }
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());

            //库存
            ResponseVo<List<WareSkuEntity>> wareSkuEntitiesBySkuId = wmsClient.queryWareSkuEntitiesBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareSkuEntitiesBySkuId.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            //营销信息
            ResponseVo<List<ItemSaleVo>> salesBySkuId = smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> saleVos = salesBySkuId.getData();
            String sales = JSON.toJSONString(saleVos);
            cart.setSales(sales);

            //销售属性
            ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValueBySkuId = pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = querySaleAttrValueBySkuId.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));

            //新增到Mysql和redis
            cartAsyncService.insertCart(cart);
        }
        //无论更新还是新增购物车，redis都是一样的操作
        hashOps.put(skuIdString, JSON.toJSONString(cart));
    }

    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = "";
        //外层的Key
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        return userId;
    }

    public Cart queryCartBySkuId(Long skuId, Integer count) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        //获得内层的Map对象
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())) {
            String json = hashOps.get(skuId.toString()).toString();
            Cart cart = JSON.parseObject(json, Cart.class);
            cart.setCount(new BigDecimal(count));
            return cart;
        } else {
            throw new CartException("当前用户不包含购物车记录");
        }
    }

    public List<Cart> queryCarts() {
        //1.获取userKey,查询未登录购物车
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        BoundHashOperations<String, Object, Object> unLoginHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userKey);
        List<Object> unLoginCartJson = unLoginHashOps.values();
        List<Cart> unLoginCarts = null;
        if (!CollectionUtils.isEmpty(unLoginCartJson)) {
            unLoginCarts = unLoginCartJson.stream().map(json -> JSON.parseObject(json.toString(), Cart.class)).collect(Collectors.toList());
        }

        //2.获取userId,判断userId是否为空，为空说明为未登录，直接返回未登录的购物车
        Long userId = userInfo.getUserId();
        if (userId == null) {
            return unLoginCarts;
        }

        //3.userId不为空，合并未登录的购物车到登录状态的购物车
        BoundHashOperations<String, Object, Object> loginHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId.toString());
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
            //遍历未登录购物车，合并到登录购物车
            unLoginCarts.forEach(cart -> {
                String skuIdString = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
                if (loginHashOps.hasKey(skuIdString)) {
                    //如果登录状态购物车包含该记录，则累加数量
                    String cartsJson = loginHashOps.get(skuIdString).toString();
                    cart = JSON.parseObject(cartsJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));
                    //保存到redis，异步保存到mysql
                    cartAsyncService.updateCart(userId.toString(), cart.getSkuId(), cart);
                } else {
                    //不包含，新增一条
                    cart.setUserId(userId.toString());
                    cartAsyncService.insertCart(cart);
                }
                loginHashOps.put(skuIdString, JSON.toJSONString(cart));
            });

            //4.删除未登录的购物车
            redisTemplate.delete(KEY_PREFIX + userKey);
            cartAsyncService.deleteCartByUserId(userKey);
        }
        //5.查询登录状态的购物车返回
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)) {
            return loginCartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class)).collect(Collectors.toList());
        }
        return null;
    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        //判断当前用户购物车是否有记录
        if (hashOps.hasKey(cart.getSkuId().toString())) {
            BigDecimal count = cart.getCount();
            String json = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(json, Cart.class);
            cart.setCount(count);
            //更新redis和mysql
            cartAsyncService.updateCart(userId, cart.getSkuId(), cart);
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            return;
        }
        throw new CartException("该用户购物车不含有记录！");
    }

    public void updateStatus(Cart cart) {
        String userId = this.getUserId();
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        //判断当前用户购物车是否有记录
        if (hashOps.hasKey(cart.getSkuId().toString())) {
            Boolean check = cart.getCheck();
            String json = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(json, Cart.class);
            cart.setCheck(check);
            //更新redis和mysql
            cartAsyncService.updateCart(userId, cart.getSkuId(), cart);
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            return;
        }
        throw new CartException("该用户购物车不含有记录！");
    }

    public void deleteCart(Long skuId) {
        String userId = this.getUserId();
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        hashOps.delete(skuId.toString());
        cartAsyncService.deleteCartByUserIdAndSkuId(userId, skuId);
    }
}
