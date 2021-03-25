package com.atguigu.gmall.gateway.filters;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        return (ServerWebExchange exchange, GatewayFilterChain chain) -> {
            System.out.println("我是局部过滤器，我只拦截配置了该过滤器的服务。 pathes = " + config.pathes);

            // 获取请求及响应对象 ServerHttpRequest --> HttpServletRequest
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // 1.获取当前请求的路径，判断是否在拦截名单中，不在直接放行
            String curPath = request.getURI().getPath();
            List<String> pathes = config.pathes;
            if (pathes.stream().anyMatch(path -> StringUtils.indexOf(curPath, path) < 0)) {
                return chain.filter(exchange);
            }

            // 2.获取请求中的jwt：cookie（同步） 头信息（异步）

            // 3.判断jwt类型的token是否为空，重定向到登录页面。

            // 4.尝试解析jwt，如果出现异常则重定向到登录页面

            // 5.获取载荷中的ip地址 和 当前请求的ip地址 比较，不同则重定向到登录


            // 6.把用户信息传递给后续服务（请求头信息）

            // 7.放行
            return chain.filter(exchange);
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("pathes");
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Data
    public static class PathConfig {
        private List<String> pathes;
    }
}
