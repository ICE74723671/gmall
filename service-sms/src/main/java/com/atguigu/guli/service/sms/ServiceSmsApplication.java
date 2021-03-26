package com.atguigu.guli.service.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * description:
 *
 * @author Ice on 2021/2/25 in 13:57
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan({"com.atguigu.guli"})
@EnableDiscoveryClient
public class ServiceSmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceSmsApplication.class,args);
    }
}
