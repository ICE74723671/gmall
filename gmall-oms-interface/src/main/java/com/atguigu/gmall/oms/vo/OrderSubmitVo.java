package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/31 in 18:15
 */
@Data
public class OrderSubmitVo {

    //防重唯一标识
    private String orderToken;

    //用户选中的收货地址
    private UserAddressEntity address;

    //支付方式
    private Integer payType;

    //配送方式，快递公司
    private String deliveryCompany;

    //购买积分
    private Integer bounds;

    //总价 验总价
    private BigDecimal totalPrice;

    //送货清单
    private List<OrderItemVo> items;
}
