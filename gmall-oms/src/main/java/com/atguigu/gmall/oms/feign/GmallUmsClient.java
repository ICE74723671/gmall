package com.atguigu.gmall.oms.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/3/31 in 0:19
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
