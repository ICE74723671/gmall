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

        //根据cid进行分组
        List<AttrGroupEntity> attrGroupEntityList = baseMapper.selectList(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));

        if (CollectionUtils.isEmpty(attrGroupEntityList)) {
            return null;
        }

        //遍历分组查询每个组的attr
        return attrGroupEntityList.stream().map(group -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setGroupName(group.getName());
            itemGroupVo.setGroupId(group.getId());

            List<AttrEntity> attrEntityList = attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", group.getId()));
            if (!CollectionUtils.isEmpty(attrEntityList)) {
                List<Long> attrIds = attrEntityList.stream().map(AttrEntity::getId).collect(Collectors.toList());
                //attrId结合spuId查询规格参数
                List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().in("attr_id", attrIds));
                //attrId结合skuId查询规格参数
                List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().in("attr_id", attrIds));

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