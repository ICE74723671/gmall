package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/4/1 in 23:54
 */
@Component
public class StockListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    private static final String KEY_PREFIX = "stock:ware:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK_UNLOCK_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.failure", "stock.unlock"}
    ))
    public void unLockStock(String orderToken, Channel channel, Message message) throws IOException {
        if (StringUtils.isBlank(orderToken)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }

        String json = redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (StringUtils.isBlank(json)) {
            //库存缓存为空，直接确认消息，并返回
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        //把json数据反序列化为库存锁定的集合
        List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
        if (CollectionUtils.isEmpty(skuLockVos)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        skuLockVos.forEach(skuLockVo -> {
            wareSkuMapper.unlock(skuLockVo.getSkuId(), skuLockVo.getCount());
        });

        redisTemplate.delete(KEY_PREFIX + orderToken);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK_MINUS_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minusStock(String orderToken, Channel channel, Message message) throws IOException {
        if (StringUtils.isBlank(orderToken)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }

        String json = redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (StringUtils.isBlank(json)) {
            //库存缓存为空，直接确认消息，并返回
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        //把json数据反序列化为库存锁定的集合
        List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
        if (CollectionUtils.isEmpty(skuLockVos)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        skuLockVos.forEach(skuLockVo -> {
            wareSkuMapper.minus(skuLockVo.getSkuId(), skuLockVo.getCount());
        });

        //删除缓存
        redisTemplate.delete(KEY_PREFIX + orderToken);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
