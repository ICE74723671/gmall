package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/9 in 20:35
 */
@Data
public class SkuVo extends SkuEntity {

    //积分
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    private List<Integer> work;

    //满减
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer fullAddOther;

    //打折
    private Integer fullCount;
    private BigDecimal discount;
    private Integer ladderAddOther;

    private List<String> images;

    private List<SkuAttrValueEntity> saleAttrs;
}
