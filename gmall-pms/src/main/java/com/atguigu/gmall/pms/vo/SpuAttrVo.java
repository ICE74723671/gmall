package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/9 in 20:29
 */
public class SpuAttrVo extends SpuAttrValueEntity {

    public void setValueSelected(List<String> valueSelected) {
        if (Collections.isEmpty(valueSelected)){
            return;
        }
        this.setAttrValue(StringUtils.join(valueSelected, ","));
    }
}
