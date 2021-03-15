package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/3/15 in 18:15
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
