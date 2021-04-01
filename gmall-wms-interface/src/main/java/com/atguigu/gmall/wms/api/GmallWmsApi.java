package com.atguigu.gmall.wms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/15 in 18:12
 */
public interface GmallWmsApi {

    //查询某个sku库存
    @GetMapping("wms/waresku/sku/{skuId}")
    public ResponseVo<List<WareSkuEntity>> queryWareSkuEntitiesBySkuId(@PathVariable("skuId") Long skuId);

    @PostMapping("wms/waresku/check/lock/{orderToken}")
    public ResponseVo<List<SkuLockVo>> checkAndLock(@RequestBody List<SkuLockVo> lockVos, @PathVariable("orderToken") String orderToken);
}

