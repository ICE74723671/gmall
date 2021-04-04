package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
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
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.IRObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

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
        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, orderToken, 1, TimeUnit.HOURS);

        return confirmVo;
    }

    public void submit(OrderSubmitVo orderSubmitVo) {

        String orderToken = orderSubmitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)) {
            throw new OrderException("非法提交！");
        }
        //1.（同一订单）防重,lua脚本保证原子性
        String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else  return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag) {
            throw new OrderException("请勿重复提交！");
        }

        //2.验总价
        BigDecimal totalPrice = orderSubmitVo.getTotalPrice();
        //获取送货清单
        List<OrderItemVo> items = orderSubmitVo.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("您没有要购买的商品");
        }
        //获取实时总价
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                return skuEntity.getPrice().multiply(item.getCount());
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();

        if (currentTotalPrice.compareTo(totalPrice) != 0) {
            throw new OrderException("页面过期，请刷新重试");
        }

        //3.验库存并锁库存
        List<SkuLockVo> skuLockVos = items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> wareResponse = wmsClient.checkAndLock(skuLockVos, orderToken);
        List<SkuLockVo> skuLockVoList = wareResponse.getData();
        if (!CollectionUtils.isEmpty(skuLockVoList)) {
            throw new OrderException(JSON.toJSONString(skuLockVoList));
        }

        //4.下单
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        try {
            omsClient.saveOrder(orderSubmitVo, userId);
            //下单未支付，定时关单
            rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.ttl", orderToken);
        } catch (Exception e) {
            e.printStackTrace();
            rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.failure", orderToken);
            throw new OrderException("服务器错误.....");
        }

        //5.删除购物车记录
        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        message.put("skuIds", skuIds);
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "cart.delete", message);

    }
}
