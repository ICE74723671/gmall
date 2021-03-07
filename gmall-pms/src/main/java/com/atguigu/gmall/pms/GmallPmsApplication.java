package com.atguigu.gmall.pms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * description:
 *
 * @author Ice on 2021/3/7 in 10:22
 */
@SpringBootApplication
@MapperScan("com.atguigu.gmall.pms.mapper")
@EnableFeignClients
@EnableSwagger2
public class GmallPmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(GmallPmsApplication.class, args);
    }
}
