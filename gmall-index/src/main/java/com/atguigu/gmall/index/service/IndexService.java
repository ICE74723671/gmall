package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";

    private static final String LOCK_PREFIX = "index:cates:lock:";

    public List<CategoryEntity> queryLvl1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0l);
        return listResponseVo.getData();
    }

    @GmallCache(prefix = KEY_PREFIX, timeout = 259200, random = 7000, lock = LOCK_PREFIX)
    public List<CategoryEntity> queryLvl2WithSubsByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryLvl2CatesWithSubsByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();

        return categoryEntities;
    }

    public List<CategoryEntity> queryLvl2WithSubsByPid2(Long pid) {
        // 1.???????????????????????????????????????
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseArray(json, CategoryEntity.class);
        }

        // ??????????????????????????????????????????
        RLock fairLock = this.redissonClient.getFairLock(LOCK_PREFIX + pid);
        fairLock.lock();
        try {
            // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            String json2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(json2)) {
                return JSON.parseArray(json2, CategoryEntity.class);
            }

            // 2.????????????????????????????????????????????????
            ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryLvl2CatesWithSubsByPid(pid);
            List<CategoryEntity> categoryEntities = listResponseVo.getData();

            if (CollectionUtils.isEmpty(categoryEntities)) {
                // ?????????????????????????????????????????????????????????????????????????????????
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 5, TimeUnit.MINUTES);
            } else {
                // ?????????????????????????????????????????????????????????
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 180l + new Random().nextInt(30), TimeUnit.DAYS);
            }

            return categoryEntities;
        } finally {
            fairLock.unlock();
        }
    }

    public void testLock() {
        // ?????????
        RLock lock = this.redissonClient.getFairLock("lock");
        lock.lock();

        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String numString = this.redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(numString)) {
            return;
        }
        int num = Integer.parseInt(numString);
        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

        lock.unlock();
    }

    public void testLock3() {
        // ?????????
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30);

        if (lock) {
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                return;
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            try {
                Thread.sleep(100000000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            this.testSubLock(uuid);

            // ?????????
            this.distributedLock.unlock("lock", uuid);
        }
    }

    public void testSubLock(String uuid) {
        this.distributedLock.tryLock("lock", uuid, 30);
        System.out.println("??????????????????");
        // ?????????
        this.distributedLock.unlock("lock", uuid);
    }

    public void testLock2() {
        // ?????????
        String uuid = UUID.randomUUID().toString();
        Boolean flag = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        if (!flag) {
            // ??????
            try {
                Thread.sleep(80);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            //this.redisTemplate.expire("lock", 3, TimeUnit.SECONDS);
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                return;
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // ?????????
            String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else  return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
//            if (StringUtils.equals(uuid, this.redisTemplate.opsForValue().get("lock"))){
//                this.redisTemplate.delete("lock");
//            }
        }

    }

    public void testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10, TimeUnit.SECONDS);

        System.out.println("=============================");
        //rwLock.readLock().unlock();
    }

    public void testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10, TimeUnit.SECONDS);


        //rwLock.writeLock().unlock();
    }

    public void testLatch() {
        try {
            RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
            latch.trySetCount(6);

            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testCountDown() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");

        latch.countDown();
    }
}
