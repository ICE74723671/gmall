package com.atguigu.gmall.pms.service.impl;

import com.alibaba.nacos.api.config.filter.IFilterConfig;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<AttrGroupEntity> queryAttrGroupEntitiesByCid(Long cid) {
        return this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
    }

    @Override
    public List<AttrGroupEntity> queryAttrGroupEntitiesAndAttr(Long catId) {
        List<AttrGroupEntity> attrGroupEntityList = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", catId));

        attrGroupEntityList.forEach(attrGroupEntity -> {
            List<AttrEntity> attrEntityList = attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()).eq("type", 1));
            attrGroupEntity.setAttrEntities(attrEntityList);
        });

        return attrGroupEntityList;
    }

    @Override
    public List<ItemGroupVo> queryGroupsBySpuIdAndCid(Long spuId, Long skuId, Long cid) {

        // 1.根据cid查询分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));

        if (CollectionUtils.isEmpty(attrGroupEntities)) {
            return null;
        }

        // 2.遍历分组查询每个组下的attr
        return attrGroupEntities.stream().map(group -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setGroupId(group.getId());
            itemGroupVo.setGroupName(group.getName());

            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", group.getId()));
            if (!CollectionUtils.isEmpty(attrEntities)) {
                List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
                // 3.attrId结合spuId查询规格参数对应值
                List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
                // 4.attrId结合skuId查询规格参数对应值
                List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));

                List<AttrValueVo> attrValueVos = new ArrayList<>();
                if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                    List<AttrValueVo> spuAttrValueVos = spuAttrValueEntities.stream().map(attrValue -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(attrValue, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList());
                    attrValueVos.addAll(spuAttrValueVos);
                }
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                    List<AttrValueVo> skuAttrValueVos = skuAttrValueEntities.stream().map(attrValue -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(attrValue, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList());
                    attrValueVos.addAll(skuAttrValueVos);
                }

                itemGroupVo.setAttrValues(attrValueVos);
            }
            return itemGroupVo;
        }).collect(Collectors.toList());
    }
}