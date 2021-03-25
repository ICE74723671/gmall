package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.time.Period;
import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/23 in 18:14
 */
@Data
public class ItemGroupVo {

    private Long groupId;
    private String groupName;
    private List<AttrValueVo> attrValues;
}
