package com.yayfolk.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 统一的 DeepSeek API 配置，替代分散在多个 Service 中的 @Value 注解。
 */
@Configuration
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekConfig {

    private String key;
    private String url;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
