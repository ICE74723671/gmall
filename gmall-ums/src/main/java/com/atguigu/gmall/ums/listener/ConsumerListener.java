package com.atguigu.gmall.ums.listener;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * description:
 *
 * @author Ice on 2021/3/17 in 19:18
 */
@Component
public class ConsumerListener {

    @RabbitListener(queues = "gmall-dead-queue")
    public void listener(String msg, Channel channel, Message message) throws IOException {
        try {
            //手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            System.out.println("消费者消费了消息：" + msg);
        } catch (Exception e) {
            //出现异常时，判断是够已经重试过
            if (message.getMessageProperties().getRedelivered()) {
                //已重试，直接拒接
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                //未重试，重新入队再次尝试确认
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
