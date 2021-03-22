package com.atguigu.gmall.index.service;

import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * description:
 *
 * @author Ice on 2021/3/22 in 18:57
 */
@Service
public class DistributedLockService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private Timer timer;

    public Boolean tryLock(String lockName, String uuid, Long expire) {
        String script = "if(redis.call('exists',KEYS[1])==0 or redis.call('hexists',KEYS[1],ARGV[1])==1)then\n" +
                " redis.call('hincrby',KEYS[1],ARGV[1],1);redis.call('expire',KEYS[1],ARGV[2]);return 1;else return 0 end;";

        if (!redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString())) {
            try {
                //尝试重新获取锁
                Thread.sleep(200);
                this.tryLock(lockName, uuid, expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.renewTime(lockName, uuid, expire);

        return true;
    }

    public void unLock(String lockName, String uuid) {
        String script = "if(redis.call('hexists',KEYS[1],ARGV[1])==0)then return nil elseif(redis.call('hincrby',KEYS[1],ARGV[1],-1) ==0) then redis.call('del',KEYS[1]);return 1;else return 0; end;";
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);
        //没有返回值，说明尝试解其他线程的锁
        if (result == null) {
            throw new IllegalMonitorStateException("尝试解其他线程的锁: lockName: " + lockName);
        } else if (result == 1) {
            timer.cancel();
        }
    }

    public void testLock() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.tryLock("lock", uuid, 30L);

        if (lock) {
            String numString = redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                return;
            }

            Integer num = Integer.parseInt(numString);
            num++;

            redisTemplate.opsForValue().set("num", String.valueOf(num));

            try {
                TimeUnit.SECONDS.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

//            this.testSubLock(uuid);

            this.unLock("lock", uuid);
        }
    }

    public void testSubLock(String uuid) {
        Boolean lock = this.tryLock("lock", uuid, 300L);

        if (lock) {
            this.unLock("lock", uuid);
        }
    }

    public void renewTime(String lockName, String uuid, Long expire) {
        String script = "if(redis.call('hexists',KEYS[1],ARGV[1])==1)then redis.call('expire',KEYS[1],ARGV[2]);return 1;else return 0; end;";
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
            }
        }, expire * 1000 / 3, expire * 1000 / 3);
    }

    public void testLock2() {
        RLock lock = redissonClient.getLock("lock");
        lock.lock(10, TimeUnit.SECONDS);

        String value = redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(value)) {
            System.out.println("num不存在");
            return;
        }

        int num = Integer.parseInt(value);

        redisTemplate.opsForValue().set("num", String.valueOf(++num));

//        lock.unlock();
    }

    public String read() {
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("rwlock");
        RLock rLock = rwlock.readLock();

        rLock.lock(10, TimeUnit.SECONDS);

        return redisTemplate.opsForValue().get("msg");
    }

    public String write() {
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("rwlock");
        RLock wLock = rwlock.writeLock();
        wLock.lock(10, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("msg", UUID.randomUUID().toString());
        return "成功写入内容";
    }

    public String latch() {
        RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("countdown");
        try {
            countDownLatch.trySetCount(5);
            countDownLatch.await();

            return "关门了...............";
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String countDown() {
        RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("countdown");
        countDownLatch.countDown();

        return "出来一个人";
    }
}
