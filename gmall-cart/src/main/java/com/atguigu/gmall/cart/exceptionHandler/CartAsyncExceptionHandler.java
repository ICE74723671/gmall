package com.atguigu.gmall.cart.exceptionHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@Component
public class CartAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EXCEPTION_KEY = "cart:exception";

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        //输出错误日志，或记录到数据库
        log.error("异步执行出错。方法：{}，参数：{}，异常信息：{}", method.getName(), Arrays.asList(objects), throwable.getMessage());

        BoundSetOperations<String, String> setOps = redisTemplate.boundSetOps(EXCEPTION_KEY);
        setOps.add(objects[0].toString());
    }
}