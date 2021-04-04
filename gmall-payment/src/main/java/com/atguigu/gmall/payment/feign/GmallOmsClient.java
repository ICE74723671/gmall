package com.atguigu.gmall.payment.feign;

import com.atguigu.gmall.oms.api.GmallOmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/4/4 in 12:01
 */
@FeignClient("oms-service")
public interface GmallOmsClient extends GmallOmsApi {
}
