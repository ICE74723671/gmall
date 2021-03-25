package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.esf.SPUserNotice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import sun.dc.pr.PRError;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * description:
 *
 * @author Ice on 2021/3/23 in 21:30
 */
@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    public ItemVo load(Long skuId) {

        ItemVo itemVo = new ItemVo();

        //根据skuId查询sku的信息
        CompletableFuture<SkuEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return null;
            }
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            return skuEntity;
        }, threadPoolExecutor);

        //根据cid3查询分类信息
        CompletableFuture<Void> categoryCompletableFuture = skuCompletableFuture.thenAcceptAsync(
                skuEntity -> {
                    ResponseVo<List<CategoryEntity>> categoriesByCid3 = pmsClient.queryCategoriesByCid3(skuEntity.getCategoryId());
                    List<CategoryEntity> cid3Data = categoriesByCid3.getData();
                    itemVo.setCategories(cid3Data);
                }, threadPoolExecutor);

        //根据品牌id查询品牌
        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);

        //根据spuId查询spu
        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        //根据skuId查询图片
        CompletableFuture<Void> skuImagesCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> imagesBySkuId = pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> imagesEntities = imagesBySkuId.getData();
            itemVo.setImages(imagesEntities);
        }, threadPoolExecutor);

        //根据skuId查询sku营销信息
        CompletableFuture<Void> salesCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> salesBySkuId = smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> salesBySkuIdData = salesBySkuId.getData();
            itemVo.setSales(salesBySkuIdData);
        }, threadPoolExecutor);

        //根据skuId查询sku库存信息
        CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() -> {

            ResponseVo<List<WareSkuEntity>> wareSkuEntitiesBySkuId = wmsClient.queryWareSkuEntitiesBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuEntitiesBySkuId.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity ->
                        wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);


        //根据spuId查询spu下的所有sku销售属性
        CompletableFuture<Void> saleAttrsCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> skuAttrValuesBySpuId = pmsClient.querySkuAttrValuesBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> skuAttrValuesBySpuIdData = skuAttrValuesBySpuId.getData();
            itemVo.setSaleAttrs(skuAttrValuesBySpuIdData);
        }, threadPoolExecutor);

        //当前sku的销售属性
        CompletableFuture<Void> saleAttrCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueBySkuId = pmsClient.querySearchAttrValueBySkuId(skuId);
            List<SkuAttrValueEntity> skuIdData = querySearchAttrValueBySkuId.getData();
            Map<Long, String> map = skuIdData.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue));
            itemVo.setSaleAttr(map);
        }, threadPoolExecutor);

        //根据spuId查询spu下的所有sku及销售属性的映射关系
        CompletableFuture<Void> skusJsonCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<Map<String, Object>> skusJsonResponseVo = pmsClient.querySkuJsonsBySpuId(skuEntity.getSpuId());
            Map<String, Object> skusJsonResponseVoData = skusJsonResponseVo.getData();
            itemVo.setSkuJsons(skusJsonResponseVoData);
        }, threadPoolExecutor);

        // 根据spuId查询spu的海报信息
        CompletableFuture<Void> spuImagesCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null || StringUtils.isNotBlank(spuDescEntity.getDecript())) {
                String[] images = StringUtils.split(spuDescEntity.getDecript(), ",");
                itemVo.setSpuImages(Arrays.asList(images));
            }
        }, threadPoolExecutor);

        //根据cid3 spuId skuId查询组及组下的规格参数及值
        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemGroupVo>> groupResponseVo = pmsClient.queryGroupsBySpuIdAndCid(skuEntity.getSpuId(), skuId, skuEntity.getCategoryId());
            List<ItemGroupVo> itemGroupVos = groupResponseVo.getData();
            itemVo.setGroups(itemGroupVos);
        }, threadPoolExecutor);

        CompletableFuture.allOf(categoryCompletableFuture, brandCompletableFuture, spuCompletableFuture,
                skuImagesCompletableFuture, salesCompletableFuture, storeCompletableFuture, saleAttrsCompletableFuture,
                saleAttrCompletableFuture, skusJsonCompletableFuture, spuImagesCompletableFuture, groupCompletableFuture).join();

        return itemVo;
    }
}
