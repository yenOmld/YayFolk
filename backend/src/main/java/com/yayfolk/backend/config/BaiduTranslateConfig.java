package com.yayfolk.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "baidu.translate")
public class BaiduTranslateConfig {
    private String appId;
    private String secretKey;
    private String apiKey;
    private String textTranslateUrl;
    private String imageTranslateUrl;
    private String speechTranslateUrl;
    private String ttsUrl;
}
