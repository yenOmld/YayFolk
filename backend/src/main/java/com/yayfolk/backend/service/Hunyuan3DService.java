package com.yayfolk.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 腾讯混元生3D API服务
 * 两步流程：submit 提交任务（ImageBase64） → poll query 轮询结果
 */
@Service
public class Hunyuan3DService {

    private static final Logger log = LoggerFactory.getLogger(Hunyuan3DService.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_POLL_COUNT = 30;       // 最多轮询30次
    private static final int POLL_INTERVAL_SEC = 10;    // 每次间隔10秒

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${hunyuan3d.api-key:}")
    private String apiKey;

    @Value("${hunyuan3d.base-url:https://api.ai3d.cloud.tencent.com}")
    private String baseUrl;

    @Value("${hunyuan3d.model:3.0}")
    private String model;

    public Map<String, Object> generate3D(Map<String, Object> request) {
        String imageUrl = textValue(request.get("imageUrl"));
        if (!StringUtils.hasText(imageUrl)) {
            throw new RuntimeException("imageUrl cannot be empty");
        }
        if (!StringUtils.hasText(apiKey) || "your-key-here".equals(apiKey.trim())) {
            throw new RuntimeException("Hunyuan3D API key is not configured");
        }

        String base = trimTrailingSlash(baseUrl);

        // 下载图片并转为 base64
        log.info("Downloading source image for 3D generation...");
        byte[] imageBytes = downloadImage(imageUrl);
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        log.info("Image downloaded: {} bytes, base64 length: {}", imageBytes.length, imageBase64.length());

        // Step 1: 提交生成任务
        String jobId = submitTask(base, imageBase64);
        log.info("Hunyuan3D task submitted: {}", jobId);

        // Step 2: 轮询查询直到完成
        String modelUrl = pollUntilComplete(base, jobId);
        log.info("Hunyuan3D task completed: {}, modelUrl={}", jobId, modelUrl);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modelUrl", modelUrl);
        result.put("format", "glb");
        result.put("jobId", jobId);
        return result;
    }

    /**
     * 下载外部图片
     */
    private byte[] downloadImage(String imageUrl) {
        try {
            Request request = new Request.Builder()
                .url(imageUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new RuntimeException("Failed to download image: HTTP " + response.code());
                }
                return response.body().bytes();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to download image from URL: " + e.getMessage(), e);
        }
    }

    /**
     * 提交生成任务 POST /v1/ai3d/submit
     * 请求: { model, ImageBase64, ResultFormat }
     * 响应: { Response: { RequestId, JobId } }
     */
    private String submitTask(String base, String imageBase64) {
        String endpoint = base + "/v1/ai3d/submit";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("ImageBase64", imageBase64);
        payload.put("ResultFormat", "GLB");

        try {
            String responseBody = postJson(endpoint, payload);
            JsonNode root = objectMapper.readTree(responseBody);

            // 解包 Response 层
            JsonNode resp = root.path("Response");
            if (!resp.isMissingNode()) {
                root = resp;
            }

            String jobId = textValue(root.path("JobId"));
            if (!StringUtils.hasText(jobId)) {
                throw new RuntimeException("Hunyuan3D submit response did not contain JobId, body: " + responseBody);
            }

            // 检查是否有错误
            JsonNode error = root.path("Error");
            if (!error.isMissingNode()) {
                String code = textValue(error.path("Code"));
                String message = textValue(error.path("Message"));
                throw new RuntimeException("Hunyuan3D submit error: [" + code + "] " + message);
            }

            return jobId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Hunyuan3D submit response: " + e.getMessage(), e);
        }
    }

    /**
     * 轮询查询任务 POST /v1/ai3d/query
     * 请求: { JobId }
     * 响应: { Response: { Status: "RUN"/"DONE"/"FAILED", ResultFile3Ds: [{ Type, Url }] } }
     */
    private String pollUntilComplete(String base, String jobId) {
        String endpoint = base + "/v1/ai3d/query";

        for (int i = 0; i < MAX_POLL_COUNT; i++) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(POLL_INTERVAL_SEC));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Hunyuan3D polling interrupted", e);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("JobId", jobId);

            try {
                String responseBody = postJson(endpoint, payload);
                JsonNode root = objectMapper.readTree(responseBody);

                // 解包 Response 层
                JsonNode resp = root.path("Response");
                if (!resp.isMissingNode()) {
                    root = resp;
                }

                String status = textValue(root.path("Status"));
                log.info("Hunyuan3D poll {}/{}: job={}, status={}", i + 1, MAX_POLL_COUNT, jobId, status);

                if ("DONE".equalsIgnoreCase(status)) {
                    // 从 ResultFile3Ds 中取 GLB 格式的 URL
                    JsonNode files = root.path("ResultFile3Ds");
                    if (files.isArray()) {
                        for (JsonNode file : files) {
                            String type = textValue(file.path("Type"));
                            String url = textValue(file.path("Url"));
                            if ("GLB".equalsIgnoreCase(type) && StringUtils.hasText(url)) {
                                return url;
                            }
                        }
                        // 如果没有 GLB，取第一个
                        if (files.size() > 0) {
                            String url = textValue(files.get(0).path("Url"));
                            if (StringUtils.hasText(url)) return url;
                        }
                    }
                    throw new RuntimeException("Hunyuan3D task DONE but no GLB URL found, body: " + responseBody);
                }

                if ("FAILED".equalsIgnoreCase(status)) {
                    String errorMsg = textValue(root.path("ErrorMessage"));
                    throw new RuntimeException("Hunyuan3D task FAILED: " + (errorMsg != null ? errorMsg : "unknown error"));
                }

                // RUN → 继续轮询
            } catch (IOException e) {
                log.warn("Hunyuan3D poll parse error: {}", e.getMessage());
            }
        }

        throw new RuntimeException("Hunyuan3D task timed out after " + (MAX_POLL_COUNT * POLL_INTERVAL_SEC) + " seconds, jobId=" + jobId);
    }

    private String postJson(String url, Map<String, Object> payload) {
        try {
            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.warn("Hunyuan3D API failed: HTTP {} - {}", response.code(), responseBody);
                    String errorMsg = parseErrorMessage(responseBody);
                    throw new RuntimeException("Hunyuan3D API error (HTTP " + response.code() + "): " + errorMsg);
                }
                return responseBody;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call Hunyuan3D API: " + e.getMessage(), e);
        }
    }

    private String parseErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) return "Unknown error";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // 先解包 Response
            JsonNode resp = root.path("Response");
            if (!resp.isMissingNode()) {
                JsonNode err = resp.path("Error");
                if (!err.isMissingNode()) {
                    return textValue(err.path("Code")) + ": " + textValue(err.path("Message"));
                }
            }
            String message = textValue(root.path("error").path("message"));
            if (StringUtils.hasText(message)) return message;
            message = textValue(root.path("message"));
            if (StringUtils.hasText(message)) return message;
        } catch (IOException ignored) {}
        return responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
    }

    private String textValue(Object value) {
        if (value == null) return null;
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            return node.isMissingNode() || node.isNull() ? null : node.asText();
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) return "https://api.ai3d.cloud.tencent.com";
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
