package com.yayfolk.backend.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yayfolk.backend.dto.SpeechTranslateResult;
import com.yayfolk.backend.util.BaiduTranslateUtil;
import com.yayfolk.backend.util.TencentTTSUtil;
import com.yayfolk.backend.util.TencentTranslateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechTranslateService {

    private final BaiduTranslateUtil baiduTranslateUtil;
    private final TencentTranslateUtil tencentTranslateUtil;
    private final TencentTTSUtil tencentTTSUtil;
    private final Gson gson = new Gson();

    public SpeechTranslateResult translateSpeech(byte[] audioData, String from, String to, String format) throws IOException {
        String response = baiduTranslateUtil.translateSpeech(audioData, from, to, format);
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

        if (jsonResponse.has("code")) {
            int code = jsonResponse.get("code").getAsInt();
            if (code != 0) {
                String errorMsg = jsonResponse.has("msg") ? jsonResponse.get("msg").getAsString() : "Unknown error";
                throw new IOException("Baidu API Error (" + code + "): " + errorMsg);
            }
        }

        String originalText = "";
        String translatedText = "";
        String audioBase64 = null;

        if (jsonResponse.has("data")) {
            JsonObject data = jsonResponse.getAsJsonObject("data");
            if (data.has("source")) {
                originalText = data.get("source").getAsString();
            }
            if (data.has("target")) {
                translatedText = data.get("target").getAsString();
            }
            if (data.has("target_tts")) {
                audioBase64 = data.get("target_tts").getAsString();
            }
        }

        return SpeechTranslateResult.builder()
                .originalText(originalText)
                .translatedText(translatedText)
                .sourceLanguage(from)
                .targetLanguage(to)
                .audioUrl(audioBase64 != null ? "data:audio/mp3;base64," + audioBase64 : null)
                .build();
    }

    public String textToSpeech(String text, String lang) {
        return textToSpeech(text, lang, null);
    }

    public String textToSpeech(String text, String lang, String voiceType) {
        try {
            String response = tencentTTSUtil.textToSpeech(text, lang, voiceType);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            if (jsonResponse.has("audio")) {
                String audioBase64 = jsonResponse.get("audio").getAsString();
                return "data:audio/mp3;base64," + audioBase64;
            }
        } catch (Exception e) {
            log.error("Tencent TTS failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    public SpeechTranslateResult translateText(String text, String from, String to) throws IOException {
        String response = tencentTranslateUtil.translateText(text, from, to);
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

        String translatedText = "";
        if (jsonResponse.has("trans_result")) {
            JsonArray transResult = jsonResponse.getAsJsonArray("trans_result");
            if (transResult.size() > 0) {
                JsonObject firstResult = transResult.get(0).getAsJsonObject();
                if (firstResult.has("dst")) {
                    translatedText = firstResult.get("dst").getAsString();
                }
            }
        }

        String audioBase64 = null;
        if (!translatedText.isEmpty() && isTTSSupported(to)) {
            try {
                String ttsResponse = tencentTTSUtil.textToSpeech(translatedText, to, null);
                JsonObject ttsJson = gson.fromJson(ttsResponse, JsonObject.class);
                if (ttsJson.has("audio")) {
                    audioBase64 = ttsJson.get("audio").getAsString();
                }
            } catch (Exception e) {
                log.warn("TTS failed: {}", e.getMessage());
            }
        }

        return SpeechTranslateResult.builder()
                .originalText(text)
                .translatedText(translatedText)
                .sourceLanguage(from)
                .targetLanguage(to)
                .audioUrl(audioBase64 != null ? "data:audio/mp3;base64," + audioBase64 : null)
                .build();
    }

    private boolean isTTSSupported(String lang) {
        return "zh".equals(lang) || "en".equals(lang) || "eng".equals(lang);
    }
}
