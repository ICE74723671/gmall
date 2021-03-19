package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * description:
 *
 * @author Ice on 2021/3/18 in 11:32
 */
@Component
public class SpuListener {

    @Autowired
    GmallPmsClient pmsClient;

    @Autowired
    GmallWmsClient wmsClient;

    @Autowired
    ElasticsearchRepository repository;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_SAVE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ITEM_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {

        try {
            ResponseVo<List<SkuEntity>> skuResp = pmsClient.querySkuEntitiesBySpuId(spuId);
            List<SkuEntity> skus = skuResp.getData();
            if (!CollectionUtils.isEmpty(skus)) {
                //把sku对象转换为goods对象
                List<Goods> goodsList = skus.stream().map(skuEntity -> {
                    Goods goods = new Goods();

                    //查询spu搜索属性及值
                    ResponseVo<List<SpuAttrValueEntity>> attrValueBySpuId = pmsClient.querySearchAttrValueBySpuId(spuId);
                    List<SpuAttrValueEntity> spuAttrValueEntities = attrValueBySpuId.getData();
                    List<SearchAttrValue> searchAttrValues = new ArrayList<>();
                    if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                        searchAttrValues = spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                            SearchAttrValue searchAttrValue = new SearchAttrValue();
                            searchAttrValue.setAttrId(spuAttrValueEntity.getAttrId());
                            searchAttrValue.setAttrName(spuAttrValueEntity.getAttrName());
                            searchAttrValue.setAttrValue(spuAttrValueEntity.getAttrValue());
                            return searchAttrValue;
                        }).collect(Collectors.toList());
                    }

                    //查询sku搜索属性及值
                    ResponseVo<List<SkuAttrValueEntity>> listResponseVo = pmsClient.querySearchAttrValueBySkuId(skuEntity.getId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = listResponseVo.getData();
                    List<SearchAttrValue> searchSkuAttrValues = new ArrayList<>();
                    if (!CollectionUtils.isEmpty(searchSkuAttrValues)) {
                        searchSkuAttrValues = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                            SearchAttrValue searchAttrValue = new SearchAttrValue();
                            searchAttrValue.setAttrId(skuAttrValueEntity.getAttrId());
                            searchAttrValue.setAttrName(skuAttrValueEntity.getAttrName());
                            searchAttrValue.setAttrValue(skuAttrValueEntity.getAttrValue());
                            return searchAttrValue;
                        }).collect(Collectors.toList());
                    }
                    searchAttrValues.addAll(searchSkuAttrValues);
                    goods.setSearchAttrs(searchAttrValues);

                    //查询品牌
                    ResponseVo<BrandEntity> brandEntityResponseVo = pmsClient.queryBrandById(skuEntity.getBrandId());
                    BrandEntity brand = brandEntityResponseVo.getData();
                    if (brand != null) {
                        goods.setBrandId(brand.getId());
                        goods.setBrandName(brand.getName());
                        goods.setLogo(brand.getLogo());
                    }

                    //查询分类
                    ResponseVo<CategoryEntity> categoryEntityResponseVo = pmsClient.queryCategoryById(skuEntity.getCategoryId());
                    CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                    if (categoryEntity != null) {
                        goods.setCategoryId(categoryEntity.getId());
                        goods.setCategoryName(categoryEntity.getName());
                    }

                    //查询搜索列表字段
                    ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(spuId);
                    SpuEntity spu = spuEntityResponseVo.getData();
                    if (spu != null) {
                        goods.setCreateTime(spu.getCreateTime());
                    }
                    goods.setTitle(skuEntity.getTitle());
                    goods.setSubTitle(skuEntity.getSubtitle());
                    goods.setSkuId(skuEntity.getId());
                    goods.setDefaultImage(skuEntity.getDefaultImage());
                    goods.setPrice(skuEntity.getPrice().doubleValue());

                    //查询库存
                    ResponseVo<List<WareSkuEntity>> listResponseVo1 = wmsClient.queryWareSkuEntitiesBySkuId(skuEntity.getId());
                    List<WareSkuEntity> wareSkuEntities = listResponseVo1.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> (wareSkuEntity.getStock() - wareSkuEntity.getStockLocked()) > 0));
                        goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                    }
                    return goods;
                }).collect(Collectors.toList());

                //导入索引库
                repository.saveAll(goodsList);
            }
            //手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            System.out.println("消费者消费了消息,spuId=" + spuId);
        } catch (Exception e) {
            //出现异常时，判断是够已经重试过
            if (message.getMessageProperties().getRedelivered()) {
                //已重试，直接拒接
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                //未重试，重新入队再次尝试确认
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
