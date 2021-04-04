package com.atguigu.gmall.payment.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.pojo.PayAsyncVo;
import com.atguigu.gmall.payment.pojo.PayVo;
import com.atguigu.gmall.payment.pojo.PaymentInfoEntity;
import com.atguigu.gmall.payment.pojo.UserInfo;
import com.atguigu.gmall.payment.service.PaymentService;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * description:
 *
 * @author Ice on 2021/4/4 in 12:02
 */
@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @GetMapping("alipay.html")
    @ResponseBody
    public String toAliPay(@RequestParam("orderToken") String orderToken) {
        OrderEntity orderEntity = paymentService.toPay(orderToken);
        if (orderEntity == null) {
            throw new OrderException("您的订单不存在");
        }

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (orderEntity.getUserId() != userInfo.getUserId()) {
            throw new OrderException("这个订单不属于您");
        }

        if (orderEntity.getStatus() != 0) {
            throw new OrderException("订单无效");
        }
        //调用支付宝接口
        try {
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderToken);
            payVo.setSubject("谷粒商城支付平台");
            payVo.setTotal_amount("0.01");
            //放入支付订单ID，用于异步回调的校验
            payVo.setPassback_params(paymentService.savePaymentInfo(payVo).toString());
            return alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping("pay/success")
    @ResponseBody
    public String paySuccess(PayAsyncVo payAsyncVo) {
        //1.验签
        Boolean flag = alipayTemplate.checkSignature(payAsyncVo);
        if (!flag) {
            return "failure";
        }

        //2.验签成功后，对支付内容进行二次校验
        String app_id = payAsyncVo.getApp_id();
        String out_trade_no = payAsyncVo.getOut_trade_no();
        String total_amount = payAsyncVo.getTotal_amount();
        //获取请求回调参数，查询对账记录
        String payId = payAsyncVo.getPassback_params();
        PaymentInfoEntity paymentInfoEntity = paymentService.queryPaymentInfoById(payId);
        if (!StringUtils.equals(app_id, alipayTemplate.getApp_id()) ||
                !StringUtils.equals(out_trade_no, paymentInfoEntity.getOutTradeNo()) ||
                new BigDecimal(total_amount).compareTo(paymentInfoEntity.getTotalAmount()) != 0) {

            return "failure";
        }

        //3.支付状态
        if (!StringUtils.equals("TRADE_SUCCESS", payAsyncVo.getTrade_status())) {
            return "failure";
        }

        //4.更新对账表
        paymentInfoEntity.setCallbackTime(new Date());
        paymentInfoEntity.setTradeNo(payAsyncVo.getOut_trade_no());
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(payAsyncVo));
        paymentInfoEntity.setPaymentStatus(1);
        if (paymentService.updateStatus(paymentInfoEntity) != 1) {
            return "failure";
        }

        //5.更新订单状态，减库存
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.pay", out_trade_no);

        //6.返回结果给支付宝
        return "success";
    }

    @GetMapping("pay/ok")
    public String payOk(PayAsyncVo payAsyncVo, Model model) {
        String total_amount = payAsyncVo.getTotal_amount();
        model.addAttribute("total_amount", total_amount);
        return "paysuccess";
    }

    @GetMapping("pay.html")
    public String toPay(@RequestParam("orderToken") String orderToken, Model model) {
        OrderEntity orderEntity = paymentService.toPay(orderToken);
        if (orderEntity == null) {
            throw new OrderException("您的订单不存在");
        }

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (orderEntity.getUserId() != userInfo.getUserId()) {
            throw new OrderException("这个订单不属于您");
        }

        if (orderEntity.getStatus() != 0) {
            throw new OrderException("订单无效");
        }
        model.addAttribute("orderEntity", orderEntity);
        return "pay";
    }
}
