package com.atguigu.gmall.index.config;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.*;

/**
 * description:
 *
 * @author Ice on 2021/3/22 in 20:11
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存的前缀
     * @return
     */
    String prefix() default "";

    /**
     * 设置缓存的过期时间
     * 单位：分钟
     * 默认一天
     * @return
     */
    int timeout() default 14400;

    /**
     * 防止雪崩的随机值范围
     * 单位：分钟
     * @return
     */
    int random() default 5;

    /**
     * 防止击穿，分布式锁的key
     * @return
     */
    String lock() default "lock";
}
