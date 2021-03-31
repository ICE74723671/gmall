package com.atguigu.gmall.order.service;

import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.pojo.UserInfo;
import com.atguigu.gmall.oms.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * description:
 *
 * @author Ice on 2021/3/31 in 0:22
 */
@Service
public class OrderService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "order:token:";

    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        //从拦截器中获取用户Id
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        //获取用户收货地址
        ResponseVo<List<UserAddressEntity>> addressesByUserId = umsClient.queryAddressesByUserId(userId);
        List<UserAddressEntity> addresses = addressesByUserId.getData();
        confirmVo.setAddresses(addresses);

        //查询送货清单
        ResponseVo<List<Cart>> checkedCarts = cartClient.queryCheckedCarts(userId);
        List<Cart> checkedCartsData = checkedCarts.getData();
        if (CollectionUtils.isEmpty(checkedCartsData)) {
            throw new CartException("您没有选中的购物车记录！");
        }
        List<OrderItemVo> orderItemVos = checkedCartsData.stream().map(cart -> {
            OrderItemVo orderItemVo = new OrderItemVo();
            orderItemVo.setCount(cart.getCount());
            orderItemVo.setSkuId(cart.getSkuId());

            //根据skuId查询sku
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
                orderItemVo.setPrice(skuEntity.getPrice());
                orderItemVo.setWeight(skuEntity.getWeight());
                orderItemVo.setTitle(skuEntity.getTitle());
            }

            //根据sku查询商品的销售属性
            ResponseVo<List<SkuAttrValueEntity>> saleAttrValueBySkuId = pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrValueBySkuId.getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);

            //根据skuId查询营销信息
            ResponseVo<List<ItemSaleVo>> salesBySkuId = smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> sales = salesBySkuId.getData();
            orderItemVo.setSales(sales);

            //根据skuId查询库存
            ResponseVo<List<WareSkuEntity>> wareSkuEntitiesBySkuId = wmsClient.queryWareSkuEntitiesBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareSkuEntitiesBySkuId.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            return orderItemVo;

        }).collect(Collectors.toList());
        confirmVo.setOrderItems(orderItemVos);

        //根据用户id查询用户信息
        ResponseVo<UserEntity> userEntityResponseVo = umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null) {
            confirmVo.setBounds(userEntity.getIntegration());
        }

        //防重
        String orderToken = IdWorker.getIdStr();
        confirmVo.setOrderToken(orderToken);
        redisTemplate.opsForValue().set(KEY_PREFIX+orderToken, orderToken,1, TimeUnit.HOURS);

        return confirmVo;
    }
}
