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
            throw new OrderException("????????????????????????");
        }

        lockVos.forEach(skuLockVo -> {
            this.checkLock(skuLockVo);
        });

        //????????????????????????????????????,????????????,???????????????????????????????????????
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())) {
            List<SkuLockVo> successLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            successLockVos.forEach(skuLockVo -> {
                wareSkuMapper.unlock(skuLockVo.getWareSkuId(), skuLockVo.getCount());
            });
            //???????????????????????????????????????
            return lockVos;
        }
        //????????????????????????redis,??????????????????????????????????????????????????????????????????
        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));

        //?????????????????????????????????????????????????????????
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.ttl", orderToken);
        return null;
    }

    public void checkLock(SkuLockVo skuLockVo) {
        //?????????????????????????????????????????????
        RLock lock = redissonClient.getLock(LOCK_PREFIX + skuLockVo.getSkuId());
        lock.lock();

        try {
            //????????????????????????????????????
            List<WareSkuEntity> wareSkuEntities = wareSkuMapper.check(skuLockVo.getSkuId(), skuLockVo.getCount());
            if (CollectionUtils.isEmpty(wareSkuEntities)) {
                skuLockVo.setLock(false);
                return;
            }

            //????????????????????????????????????
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