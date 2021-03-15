package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/3/15 in 18:15
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
