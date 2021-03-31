package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-03-31 20:17:17
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
