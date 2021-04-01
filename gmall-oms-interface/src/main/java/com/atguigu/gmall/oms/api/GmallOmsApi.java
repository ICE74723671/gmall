package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * description:
 *
 * @author Ice on 2021/4/1 in 16:02
 */
public interface GmallOmsApi {

    @PostMapping("oms/order/save/{userId}")
    public ResponseVo<OrderEntity> saveOrder(@RequestBody OrderSubmitVo submitVo, @PathVariable("userId")Long userId);
}
