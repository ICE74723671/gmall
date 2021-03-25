package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import jdk.nashorn.internal.scripts.JO;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter bloomFilter;

    /**
     * 获取目标方法的参数列表：joinPoint.getArgs()
     * 获取目标方法所在类：joinPoint.getTarget().getClass()
     * 获取目标方法签名：(MethodSignature)joinPoint.getSignature()
     * @param joinPoint
     */
    //@Before("execution(* com.atguigu.gmall.index.service.*.*(..))")
    //@AfterReturning(value = "execution(* com.atguigu.gmall.index.service.*.*(..))", returning = "result")
    //@AfterThrowing(value = "execution(* com.atguigu.gmall.index.service.*.*(..))", throwing = "ex")
    //@After(value = "execution(* com.atguigu.gmall.index.service.*.*(..))")
    //@Around("execution(* com.atguigu.gmall.index.service.*.*(..))")
    @Around("@annotation(GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{

        // 获取方法签名
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        // 方法对象
        Method method = signature.getMethod();
        // 获取方法上的特定注解
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        // 获取gmallcache注解中的前缀
        String prefix = gmallCache.prefix();
        // 获取目标方法的参数列表 组装缓存的key
        List<Object> args = Arrays.asList(joinPoint.getArgs());
        String key = prefix + args;

        // 通过bloomFilter判断数据是否存在
        if (!this.bloomFilter.contains(key)){
            return null;
        }

        // 查询缓存，缓存命中直接返回
        String json = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json, signature.getReturnType());
        }

        // 为了防止缓存击穿添加分布式锁
        String lock_prefix = gmallCache.lock();
        RLock lock = this.redissonClient.getLock(lock_prefix + args);
        lock.lock();

        try {
            // 再查缓存
            String json2 = this.redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(json2)){
                return JSON.parseObject(json2, signature.getReturnType());
            }

            // 远程调用或者查询数据库，放入缓存
            Object result = joinPoint.proceed(joinPoint.getArgs());

            // 放入缓存，释放分布式锁
            int timeout = gmallCache.timeout() + new Random().nextInt(gmallCache.random());
            this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), timeout, TimeUnit.MINUTES);

            return  result;
        } finally {
            lock.unlock();
        }
    }
}
