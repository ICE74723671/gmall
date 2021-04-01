package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/31 in 0:08
 */
@Data
public class OrderConfirmVo {

    private List<UserAddressEntity> addresses;//收货地址

    private List<OrderItemVo> orderItems;//送货清单

    private Integer bounds;//购物积分

    private String orderToken;//防止重复提交
}
