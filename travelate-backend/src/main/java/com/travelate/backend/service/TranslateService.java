package com.travelate.backend.service;

import com.travelate.backend.config.BaiduConfig;
import com.travelate.backend.dto.TranslateRequest;
import com.travelate.backend.dto.TranslateResponse;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TranslateService {
    
    @Autowired
    private BaiduConfig baiduConfig;
    
    private String accessToken;
    private long tokenExpireTime;
    
    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }
        
        try {
            String authUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials" +
                    "&client_id=" + baiduConfig.getApiKey() +
                    "&client_secret=" + baiduConfig.getSecretKey();
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(authUrl, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = new JSONObject(response.getBody());
                accessToken = result.getString("access_token");
                tokenExpireTime = System.currentTimeMillis() + (result.optLong("expires_in", 2592000) * 1000) - 60000;
                log.info("Successfully obtained Baidu access token");
                return accessToken;
            }
        } catch (Exception e) {
            log.error("Failed to get Baidu access token", e);
        }
        
        return null;
    }
    
    private String md5(String string) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] inputByteArray = string.getBytes();
            md.update(inputByteArray);
            byte[] mdBytes = md.digest();
            
            StringBuilder sb = new StringBuilder();
            for (byte b : mdBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 使用百度机器翻译API
     * API文档: https://cloud.baidu.com/doc/MT/s/4kqryjku9
     */
    public TranslateResponse translate(TranslateRequest request) {
        TranslateResponse response = new TranslateResponse();
        
        try {
            String text = request.getText();
            if (text == null || text.isEmpty()) {
                response.setCode(400);
                response.setMessage("Text is required");
                return response;
            }
            
            String token = getAccessToken();
            if (token == null) {
                response.setCode(500);
                response.setMessage("Failed to get access token");
                return response;
            }
            
            // 新版百度翻译API
            String apiUrl = "https://aip.baidubce.com/rpc/2.0/mt/texttrans/v1?access_token=" + token;
            
            // 构建请求参数
            JSONObject requestBody = new JSONObject();
            requestBody.put("q", text);
            requestBody.put("from", convertLangCode(request.getSourceLang()));
            requestBody.put("to", convertLangCode(request.getTargetLang()));
            requestBody.put("termIds", "");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> apiResponse = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            
            if (apiResponse.getStatusCode() == HttpStatus.OK) {
                JSONObject result = new JSONObject(apiResponse.getBody());
                
                if (result.has("result")) {
                    // 检查result是JSONArray还是JSONObject
                    Object resultObj = result.get("result");
                    if (resultObj instanceof org.json.JSONArray) {
                        JSONArray resultArray = (JSONArray) resultObj;
                        StringBuilder translatedBuilder = new StringBuilder();
                        for (int i = 0; i < resultArray.length(); i++) {
                            JSONObject transObj = resultArray.getJSONObject(i);
                            if (transObj.has("dst")) {
                                if (i > 0) {
                                    translatedBuilder.append("\n");
                                }
                                translatedBuilder.append(transObj.getString("dst"));
                            }
                        }
                        String translatedText = translatedBuilder.toString();
                        
                        response.setCode(200);
                        response.setMessage("Success");
                        response.setTranslatedText(translatedText);
                        response.setSourceLang(request.getSourceLang());
                        response.setTargetLang(request.getTargetLang());
                        
                        log.info("Translation successful");
                    } else if (resultObj instanceof org.json.JSONObject) {
                        // 直接处理JSONObject格式
                        JSONObject resultObject = (JSONObject) resultObj;
                        if (resultObject.has("trans_result")) {
                            JSONArray transArray = resultObject.getJSONArray("trans_result");
                            StringBuilder translatedBuilder = new StringBuilder();
                            for (int i = 0; i < transArray.length(); i++) {
                                JSONObject transObj = transArray.getJSONObject(i);
                                if (transObj.has("dst")) {
                                    if (i > 0) {
                                        translatedBuilder.append("\n");
                                    }
                                    translatedBuilder.append(transObj.getString("dst"));
                                }
                            }
                            String translatedText = translatedBuilder.toString();
                            
                            response.setCode(200);
                            response.setMessage("Success");
                            response.setTranslatedText(translatedText);
                            response.setSourceLang(request.getSourceLang());
                            response.setTargetLang(request.getTargetLang());
                            
                            log.info("Translation successful");
                        } else if (resultObject.has("dst")) {
                            String translatedText = resultObject.getString("dst");
                            
                            response.setCode(200);
                            response.setMessage("Success");
                            response.setTranslatedText(translatedText);
                            response.setSourceLang(request.getSourceLang());
                            response.setTargetLang(request.getTargetLang());
                            
                            log.info("Translation successful");
                        } else {
                            response.setCode(500);
                            response.setMessage("Invalid translation result format");
                        }
                    } else {
                        response.setCode(500);
                        response.setMessage("Invalid result format");
                    }
                } else {
                    String errorMsg = result.optString("error_msg", "Unknown error");
                    log.error("Baidu API error: {}", errorMsg);
                    response.setCode(500);
                    response.setMessage("Translation API error: " + errorMsg);
                }
            } else {
                response.setCode(500);
                response.setMessage("HTTP error: " + apiResponse.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Translation error", e);
            response.setCode(500);
            response.setMessage("Internal server error: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 转换语言代码为百度API支持的格式
     */
    private String convertLangCode(String langCode) {
        if (langCode == null || langCode.equals("auto")) {
            return "auto";
        }
        
        Map<String, String> langMap = new HashMap<>();
        langMap.put("zh", "zh");
        langMap.put("zh-CN", "zh");
        langMap.put("zh-TW", "cht");
        langMap.put("en", "en");
        langMap.put("ja", "jp");
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
        
        return langMap.getOrDefault(langCode, langCode);
    }
}
