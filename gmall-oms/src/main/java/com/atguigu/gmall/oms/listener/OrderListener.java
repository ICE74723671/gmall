package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * description:
 *
 * @author Ice on 2021/4/1 in 23:54
 */
@Component
public class OrderListener {

    @Autowired
    private OrderMapper orderMapper;

    @RabbitListener(bindings =@QueueBinding(
            value = @Queue(value = "ORDER_FAILURE_QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.failure"}
    ))
    public void disableOrder(String orderToken, Channel channel, Message message) throws IOException {
        if (StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }

        //标记无效订单，状态必须为0
        orderMapper.updateStatus(orderToken,0,5);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
