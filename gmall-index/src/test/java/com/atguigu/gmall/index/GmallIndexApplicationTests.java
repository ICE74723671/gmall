package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void testBloomFilter(){
        RBloomFilter<String> bloomFilter = this.redissonClient.getBloomFilter("bloomFilter");
        bloomFilter.tryInit(20, 0.3);
        bloomFilter.add("1");
        bloomFilter.add("2");
        bloomFilter.add("3");
        bloomFilter.add("4");
        bloomFilter.add("5");
        bloomFilter.add("6");
        bloomFilter.add("7");
        bloomFilter.add("8");
        bloomFilter.add("9");
        bloomFilter.add("10");
        bloomFilter.add("11");
        System.out.println(bloomFilter.contains("1"));
        System.out.println(bloomFilter.contains("3"));
        System.out.println(bloomFilter.contains("5"));
        System.out.println(bloomFilter.contains("7"));
        System.out.println(bloomFilter.contains("9"));
        System.out.println(bloomFilter.contains("11"));
        System.out.println(bloomFilter.contains("12"));
        System.out.println(bloomFilter.contains("13"));
        System.out.println(bloomFilter.contains("14"));
        System.out.println(bloomFilter.contains("15"));
        System.out.println(bloomFilter.contains("16"));
        System.out.println(bloomFilter.contains("17"));
        System.out.println(bloomFilter.contains("18"));
        System.out.println(bloomFilter.contains("19"));
        System.out.println(bloomFilter.contains("20"));
    }

    @Test
    void contextLoads() {
        this.redisTemplate.opsForValue().set("test1", "柳岩");
        System.out.println(this.redisTemplate.opsForValue().get("test1"));
    }

}
