package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-03-07 22:19:45
 */
@Mapper
public interface CouponMapper extends BaseMapper<CouponEntity> {
	
}
