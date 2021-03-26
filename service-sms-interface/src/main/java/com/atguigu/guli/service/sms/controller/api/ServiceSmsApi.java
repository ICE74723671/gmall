package com.atguigu.guli.service.sms.controller.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * description:
 *
 * @author Ice on 2021/3/26 in 15:21
 */
public interface ServiceSmsApi {

    @PostMapping("/api/sms/code")
    public void verificationCode(@RequestParam("phone") String phone);
}
