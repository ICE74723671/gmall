package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "E:\\gmall\\rsa\\ras.pub";
    private static final String priKeyPath = "E:\\gmall\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 2);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MTY4MTMwNTV9.TWfZkHN_WextYcWXH2rZsAqWEjFmcSAzJbC4I9HuUT_VD-C7III5tKWAs8wJlHahAiDqWLrjl9fSIMkRZEHLzL_-blMHI2wc6yj5RNEDZS0dDKYHBSVnJ8yq-WkiFyB9TXsCy21vuP7aUCrLrW8ctD061oFeYpnEgSB6UJCzBFhN-4IelHplOl4dxH22cU5fTeYJragDSLS1A4_1p9dQiEsLTCjyAjLlfiOLJ-xbMAhBeNCCMTEZ_lZJzkOm4O6vpAWYAPVh2XY9sYUdpsrgiMHD9MCc1BQVuWxNXxaeMMeXnmq-sUOf-bZ-Ef3c0Abmjit4u_g0hSqaztBTH8r6pA";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}