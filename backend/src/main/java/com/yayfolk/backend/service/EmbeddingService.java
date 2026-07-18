package com.yayfolk.backend.service;

import com.yayfolk.backend.ai.client.EmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Embedding 服务门面：委托给 EmbeddingClient 进行 API 调用，
 * 保留 cosineSimilarity 等静态工具方法和配置方法供旧代码兼容。
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    /** 向量相似度阈值：低于此值的条目视为不相关（1024维余弦相似度，0.5为合理门槛） */
    private static final double SIMILARITY_THRESHOLD = 0.5;
    public static final int EMBEDDING_DIMENSIONS = 1024;

    private final EmbeddingClient embeddingClient;

    public EmbeddingService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    /**
     * @deprecated 配置已通过 EmbeddingConfig 自动注入 EmbeddingClient，此方法保留用于旧代码兼容。
     */
    @Deprecated
    public void configure(String apiKey, String url, String model) {
        // EmbeddingClient 已通过 application.yml 自动配置
        logger.debug("EmbeddingService.configure() called but EmbeddingClient is auto-configured");
    }

    public boolean isEmbeddingAvailable() {
        return embeddingClient.isAvailable();
    }

    public float[] getEmbedding(String text) {
        return embeddingClient.getEmbedding(text);
    }

    public List<float[]> getEmbeddings(List<String> texts) {
        return embeddingClient.getEmbeddings(texts);
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static double getSimilarityThreshold() {
        return SIMILARITY_THRESHOLD;
    }

    public static int getEmbeddingDimensions() {
        return EMBEDDING_DIMENSIONS;
    }
}
