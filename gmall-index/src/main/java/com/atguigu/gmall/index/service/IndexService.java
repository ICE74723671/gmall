package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
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

    private static final String KEY_PREFIX = "index:cates:";

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
        //没有命中存入缓存
        List<CategoryEntity> categoryEntities = gmallPmsClient.queryCategoriesWithSub(pid).getData();
            //为了防止缓存穿透，给Null值设置极短的缓存
        if (CollectionUtils.isEmpty(categoryEntities)) {
            redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 1, TimeUnit.MINUTES);
            //为了防止缓存雪崩，设置随机过期值
        } else {
            redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 2160 + new Random().nextInt(900), TimeUnit.HOURS);
        }
        return categoryEntities;
    }
}
