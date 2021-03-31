package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.cart.api.GmallCartApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * description:
 *
 * @author Ice on 2021/3/31 in 8:17
 */
@FeignClient("cart-service")
public interface GmallCartClient extends GmallCartApi {
}
