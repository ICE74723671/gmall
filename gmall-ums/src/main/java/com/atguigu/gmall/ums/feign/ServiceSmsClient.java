package com.atguigu.gmall.ums.feign;

import com.atguigu.guli.service.sms.controller.api.ServiceSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/3/26 in 15:24
 */
@FeignClient("service-sms")
public interface ServiceSmsClient extends ServiceSmsApi {

}
