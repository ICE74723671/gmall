package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.AttrValueVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-03-07 10:50:50
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {

    List<AttrValueVo> querySkuAttrValuesBySpuId(Long spuId);

    List<Map<String, Object>> querySkuJsonsBySpuId(@Param("skuIds") List<Long> skuIds);
}
