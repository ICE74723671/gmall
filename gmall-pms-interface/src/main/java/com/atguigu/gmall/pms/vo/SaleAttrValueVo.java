package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.Set;

/**
 * description:
 *
 * @author Ice on 2021/3/23 in 18:14
 */
@Data
public class SaleAttrValueVo {

    private Long attrId;
    private String attrName;
    private Set<String> attrValue;//set集合自动去重
}
