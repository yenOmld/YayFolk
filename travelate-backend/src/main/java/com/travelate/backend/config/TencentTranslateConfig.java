package com.travelate.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tencent.translate")
public class TencentTranslateConfig {
    private String secretId;
    private String secretKey;
    private String endpoint;
    private String region;
}
