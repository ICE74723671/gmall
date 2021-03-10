package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Autowired
    private SpuDescMapper spuDescMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySupEntitiesByCatId(PageParamVo pageParamVo, Long catId) {
        QueryWrapper<SpuEntity> queryWrapper = new QueryWrapper<>();

        if (catId != 0) {
            queryWrapper.eq("category_id", catId);
        }

        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            queryWrapper.and(t -> t.like("id", key).or().like("name", key));
        }

        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                queryWrapper
        );

        return new PageResultVo(page);
    }

    @Override
    public void bigSave(SpuVo spuVo) {
//        1.保存spu表信息
//        1.1 保存spu基本信息
        spuVo.setPublishStatus(1);
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        this.save(spuVo);
        //获取spuId
        Long supId = spuVo.getId();

//        1.2 保存spu_attr_value表
        List<SpuAttrVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            spuAttrValueService.saveBatch(
                    baseAttrs.stream().map(SpuAttrVo -> {
                        SpuAttrVo.setSpuId(supId);
                        SpuAttrVo.setSort(0);
                        return SpuAttrVo;
                    }).collect(Collectors.toList()));
        }

//        1.3  保存spu_desc表
        SpuDescEntity spuDescEntity = new SpuDescEntity();
        spuDescEntity.setSpuId(supId);
        spuDescEntity.setDecript(StringUtils.join(spuVo.getSpuImages(), ","));
        spuDescMapper.insert(spuDescEntity);

//        2.保存sku表信息
        List<SkuVo> skus = spuVo.getSkus();
        if (!CollectionUtils.isEmpty(skus)) {
            skus.forEach(skuVo -> {
//                2.1 保存sku表
                SkuEntity skuEntity = new SkuEntity();
                BeanUtils.copyProperties(skuVo, skuEntity);
                skuEntity.setSpuId(supId);
                skuEntity.setCategoryId(spuVo.getCategoryId());
                skuEntity.setBrandId(spuVo.getBrandId());
                List<String> images = skuVo.getImages();
                //图片不为空，设置默认图片
                if (!CollectionUtils.isEmpty(images)) {
                    //如果用户可以指定默认图片，选择指定的默认图片
                    skuEntity.setDefaultImage(skuEntity.getDefaultImage() == null ? images.get(0) : skuEntity.getDefaultImage());
                }
                skuMapper.insert(skuEntity);
                //获取skuId
                Long skuId = skuEntity.getId();

//                2.2 保存sku_attr_value表
                List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
                if (!CollectionUtils.isEmpty(saleAttrs)) {
                    saleAttrs.forEach(skuAttrValueEntity -> {
                        skuAttrValueEntity.setSort(0);
                        skuAttrValueEntity.setSkuId(skuId);
                    });
                    skuAttrValueService.saveBatch(saleAttrs);
                }

//                2.3 保存sku_images表
                if (!CollectionUtils.isEmpty(images)) {
                    skuImagesService.saveBatch(images.stream().map(image -> {
                        SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                        skuImagesEntity.setSort(0);
                        skuImagesEntity.setSkuId(skuId);
                        skuImagesEntity.setUrl(image);
                        skuImagesEntity.setDefaultStatus(
                                StringUtils.equals(image, skuVo.getDefaultImage()) ? 1 : 0
                        );
                        return skuImagesEntity;
                    }).collect(Collectors.toList()));
                }

//                3.保存sku的营销信息
                SkuSaleVo skuSaleVo = new SkuSaleVo();
                BeanUtils.copyProperties(skuVo, skuSaleVo);
                skuSaleVo.setSkuId(skuId);
                gmallSmsClient.saveSkuSale(skuSaleVo);
            });
        }
    }

}