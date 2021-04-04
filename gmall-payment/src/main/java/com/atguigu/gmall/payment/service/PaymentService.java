package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.pojo.PayVo;
import com.atguigu.gmall.payment.pojo.PaymentInfoEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * description:
 *
 * @author Ice on 2021/4/4 in 12:04
 */
@Service
public class PaymentService {

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    public OrderEntity toPay(String orderToken) {
        return omsClient.queryOrderByOrderSn(orderToken).getData();
    }

    public Long savePaymentInfo(PayVo payVo) {
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setCreateTime(new Date());
        paymentInfoEntity.setOutTradeNo(payVo.getOut_trade_no());
        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setPaymentType(1);
        paymentInfoEntity.setSubject(payVo.getSubject());
        paymentInfoEntity.setTotalAmount(new BigDecimal(payVo.getTotal_amount()));
        paymentInfoMapper.insert(paymentInfoEntity);
        return paymentInfoEntity.getId();
    }

    public PaymentInfoEntity queryPaymentInfoById(String payId) {
        return paymentInfoMapper.selectById(payId);
    }

    public int updateStatus(PaymentInfoEntity paymentInfoEntity) {
        return paymentInfoMapper.updateById(paymentInfoEntity);
    }
}
