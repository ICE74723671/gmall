package com.atguigu.guli.service.sms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        // 确认消息是否到达交换机
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("消息没有到达交换机：" + cause);
            } else {
                System.out.println("消息到达交换机!!!");
            }
        });

        // 确认消息是否到达队列，到达队列该方法不执行
        this.rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.warn("消息没有到达队列，来自于交换机：{}，路由键：{}，消息内容：{}", exchange, routingKey, new String(message.getBody()));
        });
    }

    @Bean
    public TopicExchange exchange() {
        return ExchangeBuilder.topicExchange("gmall-sms-exchange").durable(true).build();
    }

    @Bean
    public Queue queue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "gmall-dead-exchange");
        arguments.put("x-dead-letter-routing-key", "msg.dead");
        arguments.put("x-message-ttl", 60000);
        return new Queue("gmall-sms-queue", true, false, false, arguments);
    }

    @Bean
    public Binding binding() {
        return new Binding("gmall-sms-queue", Binding.DestinationType.QUEUE, "gmall-sms-exchange", "msg.sms", null);
    }

    @Bean
    public TopicExchange deadExchange() {
        return ExchangeBuilder.topicExchange("gmall-dead-exchange").durable(true).build();
    }

    @Bean
    public Queue deadQueue() {
        return QueueBuilder.durable("gmall-dead-queue").build();
    }

    @Bean
    public Binding deadBinding() {
        return new Binding("gmall-dead-queue", Binding.DestinationType.QUEUE, "gmall-dead-exchange", "msg.dead", null);
    }
}