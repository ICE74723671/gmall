package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.oms.api.GmallOmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/4/1 in 16:00
 */
@FeignClient("oms-service")
public interface GmallOmsClient extends GmallOmsApi {
}
