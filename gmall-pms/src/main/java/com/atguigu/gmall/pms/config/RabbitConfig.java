package com.atguigu.gmall.pms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;

import javax.annotation.PostConstruct;

/**
 * description:
 *
 * @author Ice on 2021/3/18 in 11:20
 */
@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("消息没有到达交换机：" + cause);
            } else {
                System.out.println("消息到达交换机!!!");
            }
        });

        this.rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.warn("消息没有到达队列，来自于交换机：{}，路由键：{}，消息内容：{}", exchange, routingKey, new String(message.getBody()));
        });
    }
}
