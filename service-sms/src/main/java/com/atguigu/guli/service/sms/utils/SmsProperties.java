package com.atguigu.guli.service.sms.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * description:
 *
 * @author Ice on 2021/2/25 in 14:28
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.sms")
public class SmsProperties {
    String host;
    String path;
    String method;
    String appcode;
    String codeTemplate;
}
