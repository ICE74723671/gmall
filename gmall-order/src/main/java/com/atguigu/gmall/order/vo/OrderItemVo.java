package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/31 in 0:10
 */
@Data
public class OrderItemVo {

    private Long skuId;
    private String defaultImage;
    private String title;
    private List<SkuAttrValueEntity> saleAttrs;
    private BigDecimal price; // 加入购物车时的价格
    private BigDecimal count;
    private Boolean store = false; // 是否有货
    private List<ItemSaleVo> sales;
    private Integer weight;//重量
}
