package com.atguigu.gmall.auth.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/3/27 in 14:11
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
