package com.atguigu.gmall.item.service;

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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private TemplateEngine templateEngine;

    public ItemVo loadData(Long skuId) throws Exception {
        ItemVo itemVo = new ItemVo();

        // 1.根据skuId查询sku
        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            itemVo.setSkuId(skuEntity.getId());
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            return skuEntity;
        }, threadPoolExecutor);

        // 2.根据三级分类的id查询一二三级分类
        CompletableFuture<Void> catesFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<CategoryEntity>> catesResponseVo = this.pmsClient.queryLvlAllCategoriesByCid3(skuEntity.getCategoryId());
            List<CategoryEntity> categoryEntities = catesResponseVo.getData();
            itemVo.setCategories(categoryEntities);
        }, threadPoolExecutor);

        // 3.根据品牌id查询品牌
        CompletableFuture<Void> brandFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);

        // 4.根据spuId查询spu
        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        // 5.根据skuId查询营销信息（sms）
        CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            itemVo.setSales(itemSaleVos);
        }, threadPoolExecutor);

        // 6.根据skuId查询商品库存信息
        CompletableFuture<Void> storeFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuEntitiesBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);

        // 7.根据skuId查询sku的图片列表
        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> imagesResponseVo = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = imagesResponseVo.getData();
            itemVo.setImages(skuImagesEntities);
        }, threadPoolExecutor);

        // 8.根据spuId查询所有Sku的销售属性
        CompletableFuture<Void> saleAttrsFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> saleAttrsResponseVo = this.pmsClient.querySaleAttrsBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrsResponseVo.getData();
            itemVo.setSaleAttrs(saleAttrValueVos);
        }, threadPoolExecutor);

        // 9.根据skuId查询当前sku的销售属性 {3: '黑色', 4: '8G', 5: '128G'}
        CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                itemVo.setSaleAttr(skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)));
            }
        }, threadPoolExecutor);

        // 10.根据spuId查询spu下所有销售属性组合和skuId的映射关系
        CompletableFuture<Void> mappingFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<Map<String, Object>> mapResponseVo = this.pmsClient.querySaleAttrsMappingSkuIdBySpuId(skuEntity.getSpuId());
            Map<String, Object> map = mapResponseVo.getData();
            itemVo.setSkuJsons(map);
        }, threadPoolExecutor);

        // 11.根据spuId查询描述信息
        CompletableFuture<Void> descFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity descEntity = spuDescEntityResponseVo.getData();
            if (descEntity != null && StringUtils.isNotBlank(descEntity.getDecript())) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(descEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);


        //12.根据分类Id、spuId、skuId查询分组及组下规格参数和值
        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemGroupVo>> groupResponseVo = this.pmsClient.queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> itemGroupVos = groupResponseVo.getData();
            itemVo.setGroups(itemGroupVos);
        }, threadPoolExecutor);

        // 组合方法并阻塞
        CompletableFuture.allOf(catesFuture, brandFuture, spuFuture, salesFuture, storeFuture,
                imagesFuture, saleAttrsFuture, saleAttrFuture, mappingFuture,
                descFuture, groupFuture).exceptionally(t -> {
            log.error("异步任务出现了异常：{}", t.getMessage());
            return null;
        }).join();

        return itemVo;
    }

    public void createHtml(ItemVo itemVo){
        threadPoolExecutor.execute(() -> {
            try (PrintWriter printWriter = new PrintWriter("D:\\project-0923\\html\\" + itemVo.getSkuId() + ".html")) {
                // 上下文对象
                Context context = new Context();
                context.setVariable("itemVo", itemVo);

                // 文件流
                this.templateEngine.process("item", context, printWriter);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

//    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
//
//        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("这是通过CompletableFuture的supplyAsync初始化的子任务");
//            //int i = 1/0;
//            return "hello supplyAsync";
//        });
//        CompletableFuture<String> future1 = future.thenApplyAsync(t -> {
//            System.out.println("=============thenApplyAsync=============");
//            try {
//                TimeUnit.SECONDS.sleep(3);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("上一个任务的返回结果：" + t);
//            return "hello thenApplyAsync";
//        });
//        CompletableFuture<Void> future2 = future.thenAcceptAsync(t -> {
//            System.out.println("=============thenAcceptAsync=============");
//            try {
//                TimeUnit.SECONDS.sleep(4);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("上一个任务的返回结果：" + t);
//        });
//        CompletableFuture<Void> future3 = future.thenRunAsync(() -> {
//            System.out.println("=============thenRunAsync=============");
//            try {
//                TimeUnit.SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("不获取上一个任务的结果，也没有自己的结果集");
//        });
//
//        CompletableFuture.anyOf(future1, future2, future3).join();
//
////                .whenCompleteAsync((t, u) -> {
////            System.out.println("上一个任务的返回结果t: " + t);
////            System.out.println("上一个任务的异常信息u: " + u);
////        }).exceptionally(t -> {
////            System.out.println("上一个任务的异常信息t：" + t);
////            return null;
////        });
//
////        CompletableFuture.runAsync(() -> {
////            System.out.println("这是通过CompletableFuture的runAsync初始化的子任务");
////        });
//
//        System.out.println("这是main方法。。。。。。");
//
//        System.in.read();
//
////        FutureTask<String> futureTask = new FutureTask<>(new MyCallable());
////        new Thread(futureTask).start();
////        System.out.println("这是主线程获取子线程的返回结果集：" + futureTask.get());
////        System.out.println("这是主线程的业务逻辑。。。。。。。。。。。。。。。。");
//    }
}
//class MyCallable implements Callable<String>{
//    @Override
//    public String call() throws Exception {
//        System.out.println("这是callable实现多线程程序。。");
//        return "hello callable";
//    }
//}
