package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/9 in 20:21
 */
@Data
public class SpuVo extends SpuEntity {

    private List<String> spuImages;

    private List<SpuAttrVo> baseAttrs;

    private List<SkuVo> skus;
}
