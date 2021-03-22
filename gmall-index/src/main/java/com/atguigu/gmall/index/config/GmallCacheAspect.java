package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * description:
 *
 * @author Ice on 2021/3/22 in 20:11
 */
@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        //获取切点的签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取方法对象
        Method method = signature.getMethod();
        //获取方法上指定注解的对象
        GmallCache annotation = method.getAnnotation(GmallCache.class);
        //获取注解中的前缀
        String prefix = annotation.prefix();
        //获取方法的参数
        Object[] args = joinPoint.getArgs();
        String param = Arrays.asList(args).toString();
        //获取方法的返回值类型
        Class<?> returnType = method.getReturnType();

        //拦截前代码块：判断是否有缓存
        String json = redisTemplate.opsForValue().get(prefix + param);
        //判断缓存是否为空
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseObject(json, returnType);
        }

        //没有缓存，加分布式锁防止击穿
        String lock = annotation.lock();
        RLock rLock = redissonClient.getLock(lock + param);
        rLock.lock();

        //再次判断是否有缓存，可能会有别的请求放入了缓存
        String json2 = redisTemplate.opsForValue().get(prefix + param);
        //判断缓存是否为空
        if (StringUtils.isNotBlank(json2)) {
            rLock.unlock();
            return JSON.parseObject(json2, returnType);
        }

        //执行目标方法
        Object result = joinPoint.proceed(joinPoint.getArgs());

        //拦截后代码块：放入缓存，释放分布锁
        int timeout = annotation.timeout();
        int random = annotation.random();
        redisTemplate.opsForValue().set(prefix + param, JSON.toJSONString(result), timeout + new Random().nextInt(random), TimeUnit.MINUTES);
        rLock.unlock();

        return result;
    }
}
