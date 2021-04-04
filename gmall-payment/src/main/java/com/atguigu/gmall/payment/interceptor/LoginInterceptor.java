package com.atguigu.gmall.payment.interceptor;


import com.atguigu.gmall.payment.pojo.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * description:
 *
 * @author Ice on 2021/3/28 in 11:25
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    //声明线程的局部变量
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfo userInfo = new UserInfo();
        String userId = request.getHeader("userId");
        userInfo.setUserId(Long.valueOf(userId));
        THREAD_LOCAL.set(userInfo);
        return true;
    }

    public static UserInfo getUserInfo() {
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
    }
}
