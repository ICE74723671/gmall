package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.apache.commons.lang.RandomStringUtils;
import org.omg.CORBA.MARSHAL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * description:
 *
 * @author Ice on 2021/3/27 in 14:07
 */
@Service
@EnableConfigurationProperties({JwtProperties.class})
public class AuthService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private JwtProperties properties;

    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response) {
        try {
            //调用远程接口查询账户和密码是否正确
            ResponseVo<UserEntity> userEntityResponseVo = umsClient.queryUser(loginName, password);
            UserEntity userEntity = userEntityResponseVo.getData();

            //判断用户是否为空
            if (userEntity == null) {
                throw new UserException("用户或密码不正确!");
            }

            //组装载荷信息
            Map<String, Object> map = new HashMap<>();
            map.put("id", userEntity.getId());
            map.put("username", userEntity.getUsername());
            map.put("ip", IpUtils.getIpAddressAtService(request));

            //生成jwt
            String token = JwtUtils.generateToken(map, properties.getPrivateKey(), properties.getExpire());

            //把jwt放入cookie中
            CookieUtils.setCookie(request, response, properties.getCookieName(), token, properties.getExpire() * 60);
            //把unick放入cookie中
            CookieUtils.setCookie(request, response, properties.getUnick(), userEntity.getUsername(), properties.getExpire() * 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
