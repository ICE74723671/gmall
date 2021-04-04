package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String LOCK_PREFIX = "stock:lock:";

    private static final String KEY_PREFIX = "stock:ware:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<WareSkuEntity> queryWareSkuEntitiesBySkuId(Long skuId) {
        return this.list(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId));
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken) {
        if (CollectionUtils.isEmpty(lockVos)) {
            throw new OrderException("您没有选中的商品");
        }

        lockVos.forEach(skuLockVo -> {
            this.checkLock(skuLockVo);
        });

        //判断是否有锁定失败的商品,如果存在,需要解锁所有成功商品的库存
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())) {
            List<SkuLockVo> successLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            successLockVos.forEach(skuLockVo -> {
                wareSkuMapper.unlock(skuLockVo.getWareSkuId(), skuLockVo.getCount());
            });
            //锁定失败，需要返回锁定信息
            return lockVos;
        }
        //把锁定信息缓存到redis,方便未来支付成功时减库存，超时未支付解锁库存
        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));

        //锁库存之后，返回之前发送消息定时解库存
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.ttl", orderToken);
        return null;
    }

    public void checkLock(SkuLockVo skuLockVo) {
        //添加分布式锁，保证操作的原子性
        RLock lock = redissonClient.getLock(LOCK_PREFIX + skuLockVo.getSkuId());
        lock.lock();

        try {
            //验库存：本质就是查询库存
            List<WareSkuEntity> wareSkuEntities = wareSkuMapper.check(skuLockVo.getSkuId(), skuLockVo.getCount());
            if (CollectionUtils.isEmpty(wareSkuEntities)) {
                skuLockVo.setLock(false);
                return;
            }

            //锁库存：本质就是修改库存
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            if (wareSkuMapper.lock(wareSkuEntity.getId(), skuLockVo.getCount()) == 1) {
                skuLockVo.setLock(true);
                skuLockVo.setWareSkuId(wareSkuEntity.getSkuId());
            } else {
                skuLockVo.setLock(false);
            }
        } finally {
            lock.unlock();
        }
    }
}