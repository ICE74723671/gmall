package com.atguigu.gmall.item.feign;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/3/23 in 18:27
 */
@FeignClient("pms-service")
public interface GmallPmsClient {
}
