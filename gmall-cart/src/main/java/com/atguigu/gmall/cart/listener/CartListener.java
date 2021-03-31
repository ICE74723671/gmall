package com.atguigu.gmall.cart.listener;

import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import sun.dc.pr.PRError;

import java.io.IOException;
import java.security.acl.LastOwnerException;
import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/30 in 8:32
 */
@Component
public class CartListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    private static final String PRICE_PREFIX = "price:info:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("CART_PRICE_QUEUE"),
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        if (spuId == null) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        //获取spu下的所有sku
        ResponseVo<List<SkuEntity>> responseVo = pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = responseVo.getData();
        skuEntities.forEach(skuEntity -> {
            if (redisTemplate.hasKey(PRICE_PREFIX + skuEntity.getId())) {
                redisTemplate.opsForValue().set(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
            }
        });
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
