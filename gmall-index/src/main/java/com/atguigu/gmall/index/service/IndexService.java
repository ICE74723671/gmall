package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.sun.org.apache.regexp.internal.RE;
import io.swagger.models.auth.In;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.CompositeIterator;
import sun.misc.UUDecoder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * description:
 *
 * @author Ice on 2021/3/19 in 8:22
 */
@Service
public class IndexService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";

    private static final String LOCK_PREFIX = "index:cates:lock";

    public List<CategoryEntity> queryLv1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = gmallPmsClient.queryCategoriesById(0L);
        return listResponseVo.getData();
    }

    public List<CategoryEntity> queryLv2WithSubs(Long pid) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        //如果缓存命中了直接反序列化返回
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseArray(json, CategoryEntity.class);
        }

        //为了防止击穿，加入分布式锁
        RLock fairLock = redissonClient.getFairLock(LOCK_PREFIX + pid);
        fairLock.lock();

        //在获取分布式锁的过程中，可能其他线程已经把数据放入缓存，此时应该再次确认缓存中是否存在
        String json2 = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json2)) {
            return JSON.parseArray(json2, CategoryEntity.class);
        }

        List<CategoryEntity> categoryEntities = gmallPmsClient.queryCategoriesWithSub(pid).getData();
        //没有命中存入缓存
        //为了防止缓存穿透，给Null值设置极短的缓存
        try {
            if (CollectionUtils.isEmpty(categoryEntities)) {
                redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 1, TimeUnit.MINUTES);
                //为了防止缓存雪崩，设置随机过期值
            } else {
                redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 2160 + new Random().nextInt(900), TimeUnit.HOURS);
            }
            return categoryEntities;
        } finally {
            fairLock.unlock();
        }
    }

    @GmallCache(prefix = "index:cates:", timeout = 14400, random = 3600, lock = "lock")
    public List<CategoryEntity> queryLv2WithSubs2(Long pid) {
        return gmallPmsClient.queryCategoriesWithSub(pid).getData();

    }

}
