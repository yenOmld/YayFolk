package com.yayfolk.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    private static final String DEFAULT_EMBEDDING_URL = "https://api.siliconflow.cn/v1/embeddings";
    private static final String DEFAULT_EMBEDDING_MODEL = "BAAI/bge-large-zh-v1.5";
    private static final int EMBEDDING_DIMENSIONS = 1024;
    private static final double SIMILARITY_THRESHOLD = 0.3;
    private static final int MAX_BATCH_SIZE = 20;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String embeddingApiKey;
    private String embeddingUrl = DEFAULT_EMBEDDING_URL;
    private String embeddingModel = DEFAULT_EMBEDDING_MODEL;
    private boolean embeddingAvailable = false;

    public EmbeddingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void configure(String apiKey, String url, String model) {
        if (apiKey != null && !apiKey.isEmpty()) {
            this.embeddingApiKey = apiKey;
            this.embeddingAvailable = true;
            if (url != null && !url.isEmpty()) {
                this.embeddingUrl = url;
            }
            if (model != null && !model.isEmpty()) {
                this.embeddingModel = model;
            }
            logger.info("Embedding API configured: url={}, model={}", this.embeddingUrl, this.embeddingModel);
        } else {
            this.embeddingAvailable = false;
            logger.warn("No Embedding API key configured, will use keyword-based fallback");
        }
    }

    public boolean isEmbeddingAvailable() {
        return embeddingAvailable;
    }

    public float[] getEmbedding(String text) {
        if (!embeddingAvailable) {
            return null;
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("input", text);
            requestBody.put("encoding_format", "float");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + embeddingApiKey);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    embeddingUrl, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (data != null && !data.isEmpty()) {
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

    public List<float[]> getEmbeddings(List<String> texts) {
        List<float[]> results = new ArrayList<>();

        if (!embeddingAvailable) {
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
            requestBody.put("model", embeddingModel);
            requestBody.put("input", texts);
            requestBody.put("encoding_format", "float");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + embeddingApiKey);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    embeddingUrl, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (data != null) {
                    data.sort((a, b) -> {
                        int idxA = ((Number) a.get("index")).intValue();
                        int idxB = ((Number) b.get("index")).intValue();
                        return Integer.compare(idxA, idxB);
                    });

                    for (Map<String, Object> item : data) {
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
