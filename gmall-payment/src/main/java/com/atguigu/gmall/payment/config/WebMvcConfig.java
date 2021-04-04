package com.atguigu.gmall.payment.config;

import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * description:
 *
 * @author Ice on 2021/3/28 in 11:26
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //拦截所有路径
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**").excludePathPatterns("/pay/**");
    }
}
