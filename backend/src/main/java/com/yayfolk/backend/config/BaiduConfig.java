package com.yayfolk.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "baidu.ai")
public class BaiduConfig {
    private String appId;
    private String apiKey;
    private String secretKey;
    
    private OcrConfig ocr = new OcrConfig();
    private TranslateConfig translate = new TranslateConfig();
    
    @Data
    public static class OcrConfig {
        private String url;
    }
    
    @Data
    public static class TranslateConfig {
        private String url;
    }
}
