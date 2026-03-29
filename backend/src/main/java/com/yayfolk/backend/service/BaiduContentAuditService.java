package com.yayfolk.backend.service;

import com.yayfolk.backend.config.BaiduConfig;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BaiduContentAuditService {
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String TEXT_CENSOR_URL = "https://aip.baidubce.com/rest/2.0/solution/v1/text_censor/v2/user_defined";
    private static final String IMAGE_CENSOR_URL = "https://aip.baidubce.com/rest/2.0/solution/v1/img_censor/v2/user_defined";
    private static final String VIDEO_CENSOR_URL = "https://aip.baidubce.com/rest/2.0/solution/v1/video_censor/v1/user_defined";

    private final BaiduConfig baiduConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    private String accessToken;
    private long tokenExpireTimeMs;

    public BaiduContentAuditService(BaiduConfig baiduConfig) {
        this.baiduConfig = baiduConfig;
    }

    public AuditOutcome auditPostContent(String text, List<String> imageUrls, List<String> videoUrls) {
        List<AuditOutcome> outcomes = new ArrayList<AuditOutcome>();
        outcomes.add(auditText(text));

        for (String imageUrl : limitList(imageUrls, 9)) {
            outcomes.add(auditImage(imageUrl));
        }
        for (String videoUrl : limitList(videoUrls, 3)) {
            outcomes.add(auditVideo(videoUrl));
        }

        List<AuditOutcome> rejects = outcomes.stream()
                .filter(item -> item.getDecision() == Decision.REJECT)
                .collect(Collectors.toList());
        if (!rejects.isEmpty()) {
            return new AuditOutcome(Decision.REJECT, joinReasons(rejects));
        }

        List<AuditOutcome> reviews = outcomes.stream()
                .filter(item -> item.getDecision() == Decision.REVIEW)
                .collect(Collectors.toList());
        if (!reviews.isEmpty()) {
            return new AuditOutcome(Decision.REVIEW, joinReasons(reviews));
        }

        return new AuditOutcome(Decision.PASS, "AI machine review passed");
    }

    private AuditOutcome auditText(String text) {
        if (!StringUtils.hasText(text)) {
            return new AuditOutcome(Decision.PASS, "Text is empty");
        }
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<String, String>();
        payload.add("text", text);
        return requestAndParse("Text", TEXT_CENSOR_URL, payload);
    }

    private AuditOutcome auditImage(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return new AuditOutcome(Decision.PASS, "Image URL is empty");
        }
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<String, String>();
        payload.add("imgUrl", imageUrl);
        return requestAndParse("Image", IMAGE_CENSOR_URL, payload);
    }

    private AuditOutcome auditVideo(String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return new AuditOutcome(Decision.PASS, "Video URL is empty");
        }
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<String, String>();
        payload.add("videoUrl", videoUrl);
        return requestAndParse("Video", VIDEO_CENSOR_URL, payload);
    }

    private AuditOutcome requestAndParse(String scene, String apiUrl, MultiValueMap<String, String> payload) {
        String token = getAccessToken();
        if (!StringUtils.hasText(token)) {
            return new AuditOutcome(Decision.REVIEW, scene + " review unavailable: missing access token");
        }

        String requestUrl = apiUrl + "?access_token=" + token;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return new AuditOutcome(Decision.REVIEW, scene + " review failed: HTTP " + response.getStatusCodeValue());
            }
            return parseResult(scene, response.getBody());
        } catch (Exception e) {
            log.error("Baidu {} moderation request failed", scene, e);
            return new AuditOutcome(Decision.REVIEW, scene + " review request failed");
        }
    }

    private AuditOutcome parseResult(String scene, String body) {
        if (!StringUtils.hasText(body)) {
            return new AuditOutcome(Decision.REVIEW, scene + " review returned empty response");
        }

        try {
            JSONObject json = new JSONObject(body);
            if (json.has("error_code")) {
                String msg = json.optString("error_msg", "Unknown error");
                return new AuditOutcome(Decision.REVIEW, scene + " review API error: " + msg);
            }

            int conclusionType = json.optInt("conclusionType", 4);
            String conclusion = json.optString("conclusion", "");
            String detail = extractDetail(json);
            String reason = buildReason(conclusion, detail);

            if (conclusionType == 1) {
                return new AuditOutcome(Decision.PASS, scene + " passed");
            }
            if (conclusionType == 2) {
                return new AuditOutcome(Decision.REJECT, scene + " rejected: " + reason);
            }
            if (conclusionType == 3) {
                return new AuditOutcome(Decision.REVIEW, scene + " needs manual review: " + reason);
            }
            return new AuditOutcome(Decision.REVIEW, scene + " review failed: " + reason);
        } catch (Exception e) {
            log.error("Failed to parse Baidu moderation response: {}", body, e);
            return new AuditOutcome(Decision.REVIEW, scene + " review parse failed");
        }
    }

    private String extractDetail(JSONObject json) {
        JSONArray data = json.optJSONArray("data");
        if (data == null || data.length() == 0) {
            return "";
        }

        List<String> details = new ArrayList<String>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String msg = item.optString("msg", "");
            if (StringUtils.hasText(msg)) {
                details.add(msg);
            }

            JSONArray hits = item.optJSONArray("hits");
            if (hits == null) {
                continue;
            }
            for (int j = 0; j < hits.length(); j++) {
                JSONObject hit = hits.optJSONObject(j);
                if (hit == null) {
                    continue;
                }
                String words = hit.optString("words", "");
                if (StringUtils.hasText(words)) {
                    details.add(words);
                }
            }
        }

        if (details.isEmpty()) {
            return "";
        }
        return details.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .limit(3)
                .collect(Collectors.joining(" | "));
    }

    private String buildReason(String conclusion, String detail) {
        List<String> parts = new ArrayList<String>();
        if (StringUtils.hasText(conclusion)) {
            parts.add(conclusion);
        }
        if (StringUtils.hasText(detail)) {
            parts.add(detail);
        }
        if (parts.isEmpty()) {
            return "No detailed reason returned";
        }
        String merged = String.join(" ; ", parts);
        if (merged.length() > 300) {
            return merged.substring(0, 300);
        }
        return merged;
    }

    private String joinReasons(List<AuditOutcome> outcomes) {
        if (outcomes == null || outcomes.isEmpty()) {
            return "";
        }
        return outcomes.stream()
                .map(AuditOutcome::getReason)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(" ; "));
    }

    private synchronized String getAccessToken() {
        if (StringUtils.hasText(accessToken) && System.currentTimeMillis() < tokenExpireTimeMs) {
            return accessToken;
        }

        if (!StringUtils.hasText(baiduConfig.getApiKey()) || !StringUtils.hasText(baiduConfig.getSecretKey())) {
            log.warn("Baidu moderation credentials are not configured");
            return null;
        }

        String authUrl = TOKEN_URL
                + "?grant_type=client_credentials"
                + "&client_id=" + baiduConfig.getApiKey()
                + "&client_secret=" + baiduConfig.getSecretKey();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(authUrl, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                log.warn("Failed to obtain Baidu access token, status={}", response.getStatusCodeValue());
                return null;
            }

            JSONObject result = new JSONObject(response.getBody());
            if (result.has("error")) {
                log.warn("Baidu token API returned error: {}", result.optString("error_description"));
                return null;
            }
            String token = result.optString("access_token", null);
            if (!StringUtils.hasText(token)) {
                return null;
            }

            long expiresIn = result.optLong("expires_in", 2592000L);
            accessToken = token;
            tokenExpireTimeMs = System.currentTimeMillis() + expiresIn * 1000L - 60_000L;
            return accessToken;
        } catch (Exception e) {
            log.error("Failed to obtain Baidu access token", e);
            return null;
        }
    }

    private List<String> limitList(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public enum Decision {
        PASS,
        REJECT,
        REVIEW
    }

    public static class AuditOutcome {
        private final Decision decision;
        private final String reason;

        public AuditOutcome(Decision decision, String reason) {
            this.decision = decision;
            this.reason = reason;
        }

        public Decision getDecision() {
            return decision;
        }

        public String getReason() {
            return reason;
        }
    }
}
