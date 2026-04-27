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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

@Service
public class DoubaoHeritagePosterService {

    private static final Logger log = LoggerFactory.getLogger(DoubaoHeritagePosterService.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";
    private static final String DEFAULT_MODEL = "doubao-seedream-5-0-260128";
    private static final String FALLBACK_MODELS = "doubao-seedream-5-0-lite-260128,doubao-seedream-4-5-251128,doubao-seedream-4-0-250828";
    private static final Pattern DIMENSION_SIZE_PATTERN = Pattern.compile("^\\d+x\\d+$");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .callTimeout(15, TimeUnit.MINUTES)
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${doubao.ark-api-key:}")
    private String arkApiKey;

    @Value("${doubao.ark-base-url:" + DEFAULT_BASE_URL + "}")
    private String arkBaseUrl;

    @Value("${doubao.heritage-image-model:" + DEFAULT_MODEL + "}")
    private String heritageImageModel;

    @Value("${doubao.heritage-image-models:}")
    private String heritageImageModels;

    public Map<String, Object> generatePoster(Map<String, Object> request) {
        String prompt = textValue(request.get("prompt"));
        String imageUrl = textValue(request.get("imageUrl"));

        if (!StringUtils.hasText(prompt)) {
            throw new RuntimeException("Prompt cannot be empty");
        }
        if (!StringUtils.hasText(arkApiKey)) {
            throw new RuntimeException("ARK API key is not configured");
        }

        List<String> candidateModels = resolveCandidateModels();
        String endpoint = trimTrailingSlash(arkBaseUrl) + "/images/generations";
        DoubaoApiException lastModelException = null;

        for (int i = 0; i < candidateModels.size(); i++) {
            String model = candidateModels.get(i);
            Map<String, Object> payload = buildPayload(request, prompt, imageUrl, model);
            try {
                String responseBody = postJson(endpoint, payload);
                JsonNode root = objectMapper.readTree(responseBody);
                String url = extractImageUrl(root);
                if (!StringUtils.hasText(url)) {
                    throw new RuntimeException("Doubao response did not contain an image URL");
                }

                Map<String, Object> result = new LinkedHashMap<String, Object>();
                result.put("url", url);
                result.put("model", model);
                return result;
            } catch (DoubaoApiException e) {
                lastModelException = e;
                if (shouldFallbackToNextModel(e) && i < candidateModels.size() - 1) {
                    log.warn("Doubao model {} unavailable, trying next candidate: {}", model, e.getMessage());
                    continue;
                }
                throw e;
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse Doubao response: " + e.getMessage(), e);
            }
        }

        if (lastModelException != null) {
            throw lastModelException;
        }
        throw new RuntimeException("Doubao image generation unavailable");
    }

    private Map<String, Object> buildPayload(Map<String, Object> request, String prompt, String imageUrl, String model) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        if (StringUtils.hasText(imageUrl)) {
            payload.put("image", imageUrl);
        }
        payload.put("response_format", "url");
        String requestedSize = textValue(request.get("size"));
        String normalizedSize = normalizeSize(requestedSize);
        if (isSeedreamFallbackModel(model) && (!StringUtils.hasText(normalizedSize) || "adaptive".equalsIgnoreCase(normalizedSize))) {
            payload.put("size", "2k");
        } else {
            payload.put("size", normalizedSize);
        }
        payload.put("watermark", Boolean.TRUE);
        Object seed = request.get("seed");
        if (seed != null && StringUtils.hasText(String.valueOf(seed))) {
            payload.put("seed", seed);
        }
        return payload;
    }

    private boolean isSeedreamFallbackModel(String model) {
        return StringUtils.hasText(model) && model.contains("seedream-4-0-250828");
    }

    private String normalizeSize(String requestedSize) {
        if (!StringUtils.hasText(requestedSize)) {
            return "2k";
        }
        String normalized = requestedSize.trim().toLowerCase();
        if ("adaptive".equals(normalized)) {
            return "2k";
        }
        if ("2k".equals(normalized) || "3k".equals(normalized) || "4k".equals(normalized)) {
            return normalized;
        }
        if (DIMENSION_SIZE_PATTERN.matcher(normalized).matches()) {
            return normalized;
        }
        return "2k";
    }

    private String postJson(String url, Map<String, Object> payload) {
        try {
            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + arkApiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.warn("Doubao image generation failed: HTTP {} - {}", response.code(), responseBody);
                    throw new DoubaoApiException(response.code(), parseErrorMessage(responseBody, response.code()), parseErrorCode(responseBody));
                }
                return responseBody;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call Doubao API: " + e.getMessage(), e);
        }
    }

    private List<String> resolveCandidateModels() {
        LinkedHashSet<String> models = new LinkedHashSet<String>();
        addCandidateModels(models, heritageImageModel);
        addCandidateModels(models, heritageImageModels);
        addCandidateModels(models, FALLBACK_MODELS);
        return new ArrayList<String>(models);
    }

    private void addCandidateModels(LinkedHashSet<String> models, String rawModels) {
        if (!StringUtils.hasText(rawModels)) {
            return;
        }
        for (String rawModel : rawModels.split(",")) {
            if (StringUtils.hasText(rawModel)) {
                models.add(rawModel.trim());
            }
        }
    }

    private boolean shouldFallbackToNextModel(DoubaoApiException exception) {
        if (exception.statusCode == 400 && "Model.InvalidName".equalsIgnoreCase(exception.errorCode)) {
            return true;
        }
        return exception.statusCode == 403 || exception.statusCode == 404;
    }

    private String extractImageUrl(JsonNode root) {
        JsonNode data = root.path("data");
        if (data.isArray()) {
            for (JsonNode item : data) {
                String url = textValue(item.path("url"));
                if (StringUtils.hasText(url)) {
                    return url;
                }
                String b64 = textValue(item.path("b64_json"));
                if (StringUtils.hasText(b64)) {
                    return "data:image/png;base64," + b64;
                }
            }
        }
        return textValue(root.path("url"));
    }

    private String parseErrorMessage(String responseBody, int statusCode) {
        if (!StringUtils.hasText(responseBody)) {
            return "Doubao API request failed, HTTP " + statusCode;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = textValue(root.path("error").path("message"));
            if (StringUtils.hasText(message)) {
                return message;
            }
            message = textValue(root.path("message"));
            if (StringUtils.hasText(message)) {
                return message;
            }
        } catch (IOException ignored) {
        }
        return responseBody;
    }

    private String parseErrorCode(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String code = textValue(root.path("error").path("code"));
            if (StringUtils.hasText(code)) {
                return code;
            }
            code = textValue(root.path("code"));
            if (StringUtils.hasText(code)) {
                return code;
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            return node.isMissingNode() || node.isNull() ? null : node.asText();
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_BASE_URL;
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static class DoubaoApiException extends RuntimeException {
        private final int statusCode;
        private final String errorCode;

        private DoubaoApiException(int statusCode, String message, String errorCode) {
            super(message);
            this.statusCode = statusCode;
            this.errorCode = errorCode;
        }
    }
}