package com.travelate.backend.service;

import com.travelate.backend.config.BaiduConfig;
import com.travelate.backend.dto.OcrTranslateRequest;
import com.travelate.backend.dto.OcrTranslateResponse;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OcrTranslateService {
    
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
    
    /**
     * 使用百度图片翻译API - 一步完成OCR识别和翻译
     * API文档: https://cloud.baidu.com/doc/MT/s/mki483xpu
     */
    public OcrTranslateResponse ocrTranslate(OcrTranslateRequest request) {
        OcrTranslateResponse response = new OcrTranslateResponse();
        
        try {
            String imageBase64 = request.getImageBase64();
            if (imageBase64 == null || imageBase64.isEmpty()) {
                response.setCode(400);
                response.setMessage("Image data is required");
                return response;
            }
            
            String token = getAccessToken();
            if (token == null) {
                response.setCode(500);
                response.setMessage("Failed to get access token");
                return response;
            }
            
            // 构建图片翻译API URL
            String apiUrl = "https://aip.baidubce.com/file/2.0/mt/pictrans/v1?access_token=" + token;
            
            // 解码base64图片
            byte[] imageBytes = java.util.Base64.getDecoder().decode(imageBase64);
            
            // 构建multipart请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // 添加图片文件
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "image.jpg";
                }
            };
            body.add("image", imageResource);
            
            // 添加其他参数
            body.add("from", convertLangCode(request.getSourceLang()));
            body.add("to", convertLangCode(request.getTargetLang()));
            body.add("v", "3");
            body.add("paste", "0"); // 0-关闭文字贴合
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> apiResponse = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            
            if (apiResponse.getStatusCode() == HttpStatus.OK) {
                JSONObject result = new JSONObject(apiResponse.getBody());
                
                int errorCode = result.optInt("error_code", -1);
                if (errorCode == 0) {
                    // 成功
                    JSONObject data = result.getJSONObject("data");
                    String sumSrc = data.optString("sumSrc", "");
                    String sumDst = data.optString("sumDst", "");
                    String from = data.optString("from", request.getSourceLang());
                    String to = data.optString("to", request.getTargetLang());
                    
                    response.setCode(200);
                    response.setMessage("Success");
                    response.setOcrText(sumSrc);
                    response.setTranslatedText(sumDst);
                    response.setSourceLang(from);
                    response.setTargetLang(to);
                    
                    log.info("Image translation successful");
                } else {
                    // API返回错误
                    String errorMsg = result.optString("error_msg", "Unknown error");
                    log.error("Baidu API error: {} - {}", errorCode, errorMsg);
                    response.setCode(500);
                    response.setMessage("Translation API error: " + errorMsg);
                }
            } else {
                response.setCode(500);
                response.setMessage("HTTP error: " + apiResponse.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("OCR translate error", e);
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