package com.atguigu.gmall.gateway.filters;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import com.google.common.net.HttpHeaders;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import sun.dc.pr.PRError;
import sun.util.resources.cldr.yo.CurrencyNames_yo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@EnableConfigurationProperties(JwtProperties.class)
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    private JwtProperties properties;

    @Override
    public GatewayFilter apply(PathConfig config) {
        return (ServerWebExchange exchange, GatewayFilterChain chain) -> {
            System.out.println("局部过滤器, paths = " + config.paths);

            //获取请求及响应对象
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            //1.获取当前请求路径，是否在拦截名单中，不在则放行
            String curPath = request.getURI().getPath();
            List<String> paths = config.paths;
            //不是拦截路径，放行
            if (paths.stream().anyMatch(path -> StringUtils.indexOf(curPath, path) < 0)) {
                return chain.filter(exchange);
            }

            //2.获取jwt:cookie（同步），token（异步）
            String token = request.getHeaders().getFirst("token");
            //token中没有再去cookie中找
            if (StringUtils.isBlank(token)) {
                MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(properties.getCookieName())) {
                    token = cookies.getFirst(properties.getCookieName()).getValue();
                }
            }

            //3.判断jwt类型的token是否为空，重定向到登录页面
            if (StringUtils.isBlank(token)) {
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                return response.setComplete();
            }

            try {
                //4.尝试解析jwt，如果出现异常则重定向到登录页面
                Map<String, Object> map = JwtUtils.getInfoFromToken(token, properties.getPublicKey());

                //5.获取载荷中的ip地址和当前请求的ip地址，不同则重定向到登录
                String ip = map.get("ip").toString();
                String curIp = IpUtils.getIpAddressAtGateway(request);
                if (!StringUtils.equals(ip, curIp)) {
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();
                }

                //6.把用户信息传递给后续服务(请求头信息)
                request.mutate().header("userId", map.get("id").toString()).build();
                exchange.mutate().request(request).build();

                //7.放行
                return chain.filter(exchange);

            } catch (Exception e) {
                e.printStackTrace();
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                return response.setComplete();
            }

        };
    }

    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    @Data
    public static class PathConfig {
        private List<String> paths;
    }
}
