package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.AttrValueVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;

import static java.util.stream.Collectors.groupingBy;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    SkuMapper skuMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchAttrValueBySkuId(Long skuId) {
        return skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId));
    }

    @Override
    public List<SaleAttrValueVo> querySkuAttrValuesBySpuId(Long spuId) {
        List<AttrValueVo> attrValueVos = skuAttrValueMapper.querySkuAttrValuesBySpuId(spuId);
        //以attrId进行分组
        Map<Long, List<AttrValueVo>> map = attrValueVos.stream().collect(groupingBy(AttrValueVo::getAttrId));
        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        map.forEach((attrId, attrs) -> {
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            //attrId
            saleAttrValueVo.setAttrId(attrId);
            //attrName
            saleAttrValueVo.setAttrName(attrs.get(0).getAttrName());
            //attrValues
            Set<String> attrValue = attrs.stream().map(AttrValueVo::getAttrValue).collect(Collectors.toSet());
            saleAttrValueVo.setAttrValue(attrValue);
            saleAttrValueVos.add(saleAttrValueVo);
        });

        return saleAttrValueVos;
    }

    @Override
    public Map<String, Object> querySkuJsonsBySpuId(Long spuId) {
        List<Long> skuIds = getSkuIdsBySpuId(spuId);
        if (CollectionUtils.isEmpty(skuIds)) return null;
        List<Map<String, Object>> maps = baseMapper.querySkuJsonsBySpuId(skuIds);
        return maps.stream().collect(Collectors.toMap(map -> map.get("attrValues").toString(), map -> map.get("sku_id")));
    }

    private List<Long> getSkuIdsBySpuId(Long spuId) {
        //查询spu下的sku集合
        List<SkuEntity> skuEntityList = skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if (CollectionUtils.isEmpty(skuEntityList)) {
            return null;
        }

        //获取skuId集合
        List<Long> skuIds = skuEntityList.stream().map(SkuEntity::getId).collect(Collectors.toList());
        return skuIds;
    }
}