package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;

import java.util.List;

/**
 * εεεΊε­
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-03-09 18:37:09
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<WareSkuEntity> queryWareSkuEntitiesBySkuId(Long skuId);

    List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken);
}

