package com.yayfolk.backend.service;

import com.yayfolk.backend.dto.TranslateRequest;
import com.yayfolk.backend.dto.TranslateResponse;
import com.yayfolk.backend.util.BaiduTranslateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranslateService {

    private final BaiduTranslateUtil baiduTranslateUtil;

    public TranslateResponse translate(TranslateRequest request) {
        TranslateResponse response = new TranslateResponse();

        try {
            String text = request.getText();
            if (!StringUtils.hasText(text)) {
                response.setCode(400);
                response.setMessage("Text is required");
                return response;
            }

            String sourceLang = convertLangCode(request.getSourceLang());
            String targetLang = convertLangCode(request.getTargetLang());
            String rawResponse = baiduTranslateUtil.translateText(text, sourceLang, targetLang);

            if (!StringUtils.hasText(rawResponse)) {
                response.setCode(500);
                response.setMessage("Translation API returned empty response");
                return response;
            }

            JSONObject result = new JSONObject(rawResponse);
            if (result.has("error_code")) {
                String errorMsg = result.optString("error_msg", "Unknown error");
                log.error("Baidu text translation API error: {}", rawResponse);
                response.setCode(500);
                response.setMessage("Translation API error: " + errorMsg);
                return response;
            }

            String translatedText = extractTranslatedText(result);
            if (!StringUtils.hasText(translatedText)) {
                log.error("Unexpected Baidu text translation response: {}", rawResponse);
                response.setCode(500);
                response.setMessage("Invalid translation result format");
                return response;
            }

            response.setCode(200);
            response.setMessage("Success");
            response.setTranslatedText(translatedText);
            response.setSourceLang(request.getSourceLang());
            response.setTargetLang(request.getTargetLang());
            return response;
        } catch (Exception e) {
            log.error("Translation error", e);
            response.setCode(500);
            response.setMessage("Internal server error: " + e.getMessage());
            return response;
        }
    }

    private String extractTranslatedText(JSONObject result) {
        if (result.has("trans_result")) {
            Object transResult = result.get("trans_result");
            if (transResult instanceof JSONArray) {
                return joinTranslatedArray((JSONArray) transResult);
            }
            if (transResult instanceof JSONObject) {
                JSONObject transObject = (JSONObject) transResult;
                if (transObject.has("data") && transObject.get("data") instanceof JSONArray) {
                    return joinTranslatedArray(transObject.getJSONArray("data"));
                }
                if (transObject.has("dst")) {
                    return transObject.optString("dst", "");
                }
            }
        }

        if (result.has("result")) {
            Object resultObj = result.get("result");
            if (resultObj instanceof JSONArray) {
                return joinTranslatedArray((JSONArray) resultObj);
            }
            if (resultObj instanceof JSONObject) {
                JSONObject resultObject = (JSONObject) resultObj;
                if (resultObject.has("trans_result") && resultObject.get("trans_result") instanceof JSONArray) {
                    return joinTranslatedArray(resultObject.getJSONArray("trans_result"));
                }
                if (resultObject.has("dst")) {
                    return resultObject.optString("dst", "");
                }
            }
        }

        if (result.has("dst")) {
            return result.optString("dst", "");
        }

        return "";
    }

    private String joinTranslatedArray(JSONArray array) {
        StringBuilder translatedBuilder = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null || !item.has("dst")) {
                continue;
            }
            if (translatedBuilder.length() > 0) {
                translatedBuilder.append("\n");
            }
            translatedBuilder.append(item.optString("dst", ""));
        }
        return translatedBuilder.toString();
    }

    private String convertLangCode(String langCode) {
        if (!StringUtils.hasText(langCode) || "auto".equalsIgnoreCase(langCode)) {
            return "auto";
        }

        String normalized = langCode.trim().toLowerCase().replace('_', '-');
        Map<String, String> langMap = new HashMap<String, String>();
        langMap.put("zh", "zh");
        langMap.put("zh-cn", "zh");
        langMap.put("zh-hans", "zh");
        langMap.put("zh-tw", "cht");
        langMap.put("zh-hant", "cht");
        langMap.put("en", "en");
        langMap.put("ja", "jp");
        langMap.put("jp", "jp");
        langMap.put("ko", "kor");
        langMap.put("fr", "fra");
        langMap.put("de", "de");
        langMap.put("es", "spa");
        langMap.put("it", "it");
        langMap.put("pt", "pt");
        langMap.put("ru", "ru");
        langMap.put("ar", "ara");
        langMap.put("th", "th");
        langMap.put("vi", "vie");
        langMap.put("id", "id");
        langMap.put("ms", "may");
        langMap.put("tr", "tr");
        langMap.put("nl", "nl");
        langMap.put("pl", "pl");

        if (langMap.containsKey(normalized)) {
            return langMap.get(normalized);
        }
        if (normalized.contains("-")) {
            String shortCode = normalized.substring(0, normalized.indexOf('-'));
            if (langMap.containsKey(shortCode)) {
                return langMap.get(shortCode);
            }
        }
        return normalized;
    }
}