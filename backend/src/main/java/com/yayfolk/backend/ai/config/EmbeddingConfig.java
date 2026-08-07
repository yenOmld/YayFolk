package com.yayfolk.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 统一的 Embedding API 配置。
 */
@Configuration
@ConfigurationProperties(prefix = "embedding.api")
public class EmbeddingConfig {

    private String key;
    private String url = "https://api.siliconflow.cn/v1/embeddings";
    private String model = "BAAI/bge-large-zh-v1.5";

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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
