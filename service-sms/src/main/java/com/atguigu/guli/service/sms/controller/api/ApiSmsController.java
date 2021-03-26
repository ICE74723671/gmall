package com.atguigu.guli.service.sms.controller.api;


import com.atguigu.gmall.common.exception.GuliException;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.utils.RandomUtils;
import com.atguigu.guli.service.sms.utils.HttpUtils;
import com.atguigu.guli.service.sms.utils.SmsProperties;
import com.google.gson.Gson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import sun.misc.CEFormatException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * description:
 *
 * @author Ice on 2021/2/25 in 14:03
 */
@RequestMapping("/api/sms")
@Api(tags = "短信管理")
@RestController
@Slf4j
public class ApiSmsController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SmsProperties smsProperties;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("code")
    public void verificationCode(@RequestParam("phone") String phone) {
        String codeTimesPerDayKey = "code:times:perday" + phone;
        int timesPerDay = 0;
        //先判断该手机号码是否可以正常接收验证码
        //先判断一天获取验证码的次数
        Object timesPerDayObj = redisTemplate.opsForValue().get(codeTimesPerDayKey);
        if (timesPerDayObj != null) {
            System.out.println("timesPerDayObj.getClass().getName() = " + timesPerDayObj.getClass().getName());
            timesPerDay = (Integer) timesPerDayObj;
            if (timesPerDay >= 5) {
                throw new GuliException(ResultCodeEnum.SMS_SEND_ERROR_BUSINESS_LIMIT_CONTROL_PERDAY);
            }
        }
        //判断一分钟之内是否获取过验证码
        Boolean hasKey = redisTemplate.hasKey("code:times:perminute:" + phone);
        if (hasKey) {
            throw new GuliException(ResultCodeEnum.SMS_SEND_ERROR_BUSINESS_LIMIT_CONTROL);
        }

        String host = smsProperties.getHost();
        String path = smsProperties.getPath();
        String method = smsProperties.getMethod();
        String appcode = smsProperties.getAppcode();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", phone);
        //生成随机六位验证码
        String code = RandomUtils.getSixBitRandom();
        querys.put("param", "code:" + code);
        querys.put("tpl_id", smsProperties.getCodeTemplate());
        Map<String, String> bodys = new HashMap<String, String>();
        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            String content = EntityUtils.toString(response.getEntity());
            Gson gson = new Gson();
            Map map = gson.fromJson(content, Map.class);
            Object returnCode = map.get("return_code");
            System.out.println("returnCode = " + returnCode);

            if ("00000".equals(returnCode.toString())) {
                //验证码发送成功，保存到缓存10分钟
                redisTemplate.opsForValue().set("code:regist:" + phone, code, 10, TimeUnit.MINUTES);
                //一天内接收的短信数加1
                redisTemplate.opsForValue().increment(codeTimesPerDayKey);
                //如果该手机号是一天内第一次接收短信，则设置过期时间为一天，即第二天失效
                if (timesPerDay == 0) {
                    redisTemplate.expire(codeTimesPerDayKey, 1, TimeUnit.DAYS);
                }
                //限制一分钟之内不能再接收短信
                redisTemplate.opsForValue().set("code:times:perminute:" + phone, "1", 1, TimeUnit.MINUTES);
            } else {
                throw new GuliException(ResultCodeEnum.SMS_SEND_ERROR);
            }

            rabbitTemplate.convertAndSend("gmall-sms-exchange","msg.sms",code);
            System.out.println("短信服务发送了消息..................");
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new GuliException(ResultCodeEnum.SMS_SEND_ERROR);
        }
    }
}
