package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/3/10 in 11:24
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {

}
