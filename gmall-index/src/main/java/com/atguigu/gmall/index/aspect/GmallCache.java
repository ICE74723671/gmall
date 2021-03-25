package com.atguigu.gmall.index.aspect;

import org.springframework.transaction.TransactionDefinition;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存key的前缀
     * @return
     */
    String prefix() default "";

    /**
     * 缓存的过期时间，单位：min
     * 默认一天
     * @return
     */
    int timeout() default 1440;

    /**
     * 防止缓存雪崩，给缓存时间添加随机值范围，单位：min
     * 默认50
     * @return
     */
    int random() default 50;

    /**
     * 为了防止缓存击穿，添加分布式锁，这里可以指定锁前缀
     *
     * @return
     */
    String lock() default "lock:";
}
