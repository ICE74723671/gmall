package com.atguigu.gmall.wms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
}
