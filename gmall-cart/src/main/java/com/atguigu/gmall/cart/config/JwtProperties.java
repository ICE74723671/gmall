package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * description:
 *
 * @author Ice on 2021/3/27 in 11:02
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    private String pubKeyPath;
    private String cookieName;
    private String userKey;
    private Integer expireTime;

    private PublicKey publicKey;

    //初始化公钥和私钥
    @PostConstruct
    public void init() {
        try {
            File pubFile = new File(pubKeyPath);
            //如果公钥私钥任意一个不存在，都要重新生成
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            log.error("生成私钥和公钥出错");
            e.printStackTrace();
        }
    }
}
