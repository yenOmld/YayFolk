package com.yayfolk.backend.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.ai.config.EmbeddingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 统一的 Embedding API 客户端，封装 SiliconFlow Embedding API 调用。
 */
@Component
public class EmbeddingClient {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingClient.class);

    private static final int MAX_BATCH_SIZE = 20;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EmbeddingConfig config;

    public EmbeddingClient(RestTemplate restTemplate, ObjectMapper objectMapper, EmbeddingConfig config) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    /**
     * 获取单个文本的 Embedding 向量。
     */
    public float[] getEmbedding(String text) {
        if (!isAvailable()) {
            return null;
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("input", text);
            requestBody.put("encoding_format", "float");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + config.getKey());

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getUrl(), HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (data != null && !data.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    List<Number> embeddingList = (List<Number>) data.get(0).get("embedding");
                    float[] embedding = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        embedding[i] = embeddingList.get(i).floatValue();
                    }
                    return embedding;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get embedding for text: {}", text.substring(0, Math.min(text.length(), 50)), e);
        }

        return null;
    }

    /**
     * 批量获取 Embedding 向量。
     */
    public List<float[]> getEmbeddings(List<String> texts) {
        List<float[]> results = new ArrayList<>();

        if (!isAvailable()) {
            for (int i = 0; i < texts.size(); i++) {
                results.add(null);
            }
            return results;
        }

        for (int i = 0; i < texts.size(); i += MAX_BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + MAX_BATCH_SIZE, texts.size()));
            List<float[]> batchResults = getEmbeddingsBatch(batch);
            results.addAll(batchResults);
        }

        return results;
    }

    private List<float[]> getEmbeddingsBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("input", texts);
            requestBody.put("encoding_format", "float");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + config.getKey());

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getUrl(), HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (data != null) {
                    // Sort by index to maintain order
                    data.sort((a, b) -> {
                        int idxA = ((Number) a.get("index")).intValue();
                        int idxB = ((Number) b.get("index")).intValue();
                        return Integer.compare(idxA, idxB);
                    });

                    for (Map<String, Object> item : data) {
                        @SuppressWarnings("unchecked")
                        List<Number> embeddingList = (List<Number>) item.get("embedding");
                        float[] embedding = new float[embeddingList.size()];
                        for (int i = 0; i < embeddingList.size(); i++) {
                            embedding[i] = embeddingList.get(i).floatValue();
                        }
                        results.add(embedding);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get embeddings batch", e);
            for (int i = 0; i < texts.size(); i++) {
                results.add(null);
            }
        }

        return results;
    }

    /**
     * Embedding API 是否可用（已配置 API Key）。
     */
    public boolean isAvailable() {
        return config.getKey() != null && !config.getKey().isEmpty();
    }
}
