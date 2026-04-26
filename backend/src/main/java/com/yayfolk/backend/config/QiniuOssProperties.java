package com.yayfolk.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "qiniuyun.oss")
public class QiniuOssProperties {
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String domainName;
    private String customSuffix;
}