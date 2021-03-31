package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallSmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.sms.api.GmallSmsApi;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.api.GmallUmsApi;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.ORB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.OffsetTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private OrderItemService itemService;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId) {
        //保存订单表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(userId);
        ResponseVo<UserEntity> userEntityResponseVo = umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        orderEntity.setUsername(userEntity.getUsername());

        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setPayAmount(submitVo.getTotalPrice());
        orderEntity.setIntegrationAmount(new BigDecimal(submitVo.getBounds()).divide(new BigDecimal(10)));
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());

        UserAddressEntity address = submitVo.getAddress();
        if (address != null) {
            orderEntity.setReceiverAddress(address.getAddress());
            orderEntity.setReceiverCity(address.getCity());
            orderEntity.setReceiverName(address.getName());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverRegion(address.getRegion());
        }

        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());

        //保存订单详情
        List<OrderItemVo> items = submitVo.getItems();
        if (!CollectionUtils.isEmpty(items)) {
            List<OrderItemEntity> itemEntities = items.stream().map(orderItemVo -> {
                OrderItemEntity itemEntity = new OrderItemEntity();
                itemEntity.setOrderId(orderEntity.getId());
                itemEntity.setOrderSn(submitVo.getOrderToken());
                //sku相关信息
                ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(orderEntity.getId());
                SkuEntity skuEntity = skuEntityResponseVo.getData();
                if (skuEntity != null) {
                    itemEntity.setSkuId(skuEntity.getId());
                    itemEntity.setSkuName(skuEntity.getName());
                    itemEntity.setSkuPic(skuEntity.getDefaultImage());
                    itemEntity.setSkuPrice(skuEntity.getPrice());
                    itemEntity.setSkuQuantity(orderItemVo.getCount().intValue());

                    //spu相关信息
                    ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(skuEntity.getId());
                    SpuEntity spuEntity = spuEntityResponseVo.getData();
                    if (spuEntity != null) {
                        itemEntity.setSpuId(spuEntity.getId());
                        itemEntity.setSpuName(spuEntity.getName());
                    }

                    //spu描述信息
                    ResponseVo<SpuDescEntity> spuDescEntityResponseVo = pmsClient.querySpuDescById(skuEntity.getSpuId());
                    SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
                    if (spuDescEntity != null) {
                        itemEntity.setSpuPic(spuDescEntity.getDecript());
                    }

                    //品牌信息
                    ResponseVo<BrandEntity> brandEntityResponseVo = pmsClient.queryBrandById(skuEntity.getBrandId());
                    BrandEntity brandEntity = brandEntityResponseVo.getData();
                    if (brandEntity != null) {
                        itemEntity.setSpuBrand(brandEntity.getName());
                    }
                }

                //查询销售属性
                ResponseVo<List<SkuAttrValueEntity>> saleAttrValueBySkuId = pmsClient.querySaleAttrValueBySkuId(orderItemVo.getSkuId());
                List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrValueBySkuId.getData();
                itemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));

                //TODO:查询积分信息

                return itemEntity;
            }).collect(Collectors.toList());
            itemService.saveBatch(itemEntities);
        }
        return orderEntity;
    }

}