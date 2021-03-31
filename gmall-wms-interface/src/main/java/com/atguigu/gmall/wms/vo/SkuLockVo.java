package com.atguigu.gmall.wms.vo;

import javafx.beans.binding.BooleanExpression;
import lombok.Data;

/**
 * description:
 *
 * @author Ice on 2021/3/31 in 18:47
 */
@Data
public class SkuLockVo {

    private Long skuId;
    private Integer count;

    private Boolean lock;//锁定状态
    private Long wareSkuId;//锁定成功时的仓库id，用于异常时解锁
}

