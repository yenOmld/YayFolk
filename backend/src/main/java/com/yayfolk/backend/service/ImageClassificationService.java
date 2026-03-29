package com.yayfolk.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.dto.ResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImageClassificationService {

    @Value("${image.classifier.service-url:http://127.0.0.1:5001}")
    private String serviceUrl;

    @Value("${image.classifier.topk:3}")
    private int topk;

    @Value("${image.classifier.auto-tag-confidence-threshold:0.60}")
    private double autoTagConfidenceThreshold;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseDto health() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("serviceUrl", serviceUrl);
        result.put("topk", topk);
        result.put("threshold", autoTagConfidenceThreshold);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(serviceUrl + "/health", String.class);
            result.put("available", response.getStatusCode().is2xxSuccessful());
            if (response.hasBody() && StringUtils.hasText(response.getBody())) {
                JsonNode body = objectMapper.readTree(response.getBody());
                result.put("service", objectMapper.convertValue(body, Map.class));
            }
            return ResponseDto.success(result);
        } catch (Exception e) {
            result.put("available", false);
            result.put("error", e.getMessage());
            return ResponseDto.success(result);
        }
    }

    public ResponseDto predictTags(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseDto.error(400, "Please upload an image file.");
        }

        String contentType = image.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            return ResponseDto.error(400, "Only image files are supported.");
        }

        try {
            JsonNode root = invokeClassifier(image);
            List<Prediction> predictions = parsePredictions(root.path("predictions"));
            if (predictions.isEmpty()) {
                return ResponseDto.error(500, "No classification result was returned.");
            }

            Prediction primary = predictions.get(0);
            List<String> autoTags = new ArrayList<String>();
            if (primary.getConfidence() >= autoTagConfidenceThreshold && StringUtils.hasText(primary.getTag())) {
                autoTags.add(primary.getTag());
            }

            List<Map<String, Object>> predictionItems = new ArrayList<Map<String, Object>>();
            for (Prediction prediction : predictions) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("rank", prediction.getRank());
                item.put("tag", prediction.getTag());
                item.put("confidence", prediction.getConfidence());
                predictionItems.add(item);
            }

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("primaryTag", primary.getTag());
            result.put("confidence", primary.getConfidence());
            result.put("autoTags", autoTags);
            result.put("predictions", predictionItems);
            result.put("threshold", autoTagConfidenceThreshold);
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(500, "Image classification failed: " + e.getMessage());
        }
    }

    private JsonNode invokeClassifier(MultipartFile image) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("image", new NamedByteArrayResource(image.getBytes(), resolveFilename(image)));
        body.add("topk", String.valueOf(Math.max(1, topk)));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(serviceUrl + "/predict", HttpMethod.POST, requestEntity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
            throw new RuntimeException("Classifier service returned an empty response.");
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        if (root.hasNonNull("error")) {
            throw new RuntimeException(root.path("error").asText());
        }
        return root;
    }

    private List<Prediction> parsePredictions(JsonNode items) {
        List<Prediction> predictions = new ArrayList<Prediction>();
        if (items == null || !items.isArray()) {
            return predictions;
        }

        for (JsonNode item : items) {
            predictions.add(new Prediction(
                    item.path("rank").asInt(predictions.size() + 1),
                    item.path("tag").asText(),
                    item.path("confidence").asDouble()
            ));
        }
        return predictions;
    }

    private String resolveFilename(MultipartFile image) {
        String originalFilename = image.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            return "upload.jpg";
        }
        return originalFilename;
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private static class Prediction {
        private final int rank;
        private final String tag;
        private final double confidence;

        private Prediction(int rank, String tag, double confidence) {
            this.rank = rank;
            this.tag = tag;
            this.confidence = confidence;
        }

        private int getRank() {
            return rank;
        }

        private String getTag() {
            return tag;
        }

        private double getConfidence() {
            return confidence;
        }
    }
}
