package com.atguigu.gmall.sms.vo;

import lombok.Data;

/**
 * description:
 *
 * @author Ice on 2021/3/23 in 18:14
 */
@Data
public class ItemSaleVo  {

    private String type;//营销类型：打折，满减，积分
    private String desc;//具体描述
}
