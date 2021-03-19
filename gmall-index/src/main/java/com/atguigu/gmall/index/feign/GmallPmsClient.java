package com.atguigu.gmall.index.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/3/19 in 8:23
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
