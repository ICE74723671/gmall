package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.lang.management.PlatformLoggingMXBean;
import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/23 in 8:32
 */
@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GmallPmsClient pmsClient;

    private static final String KEY_PREFIX = "index:cates:[";

    @Bean
    public RBloomFilter rBloomFilter() {
        RBloomFilter<String> bloomfilter = redissonClient.getBloomFilter("index:cates:bloom");
        bloomfilter.tryInit(500L, 0.02);

        ResponseVo<List<CategoryEntity>> responseVo = pmsClient.queryCategoriesById(0L);
        List<CategoryEntity> categoryEntities = responseVo.getData();
        if (!CollectionUtils.isEmpty(categoryEntities)) {
            categoryEntities.forEach(categoryEntity -> {
                bloomfilter.add(KEY_PREFIX + categoryEntity.getId() + "]");
            });
        }
        return bloomfilter;
    }
}
