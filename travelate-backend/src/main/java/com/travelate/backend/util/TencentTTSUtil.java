package com.travelate.backend.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.travelate.backend.config.TencentTranslateConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TencentTTSUtil {

    private final TencentTranslateConfig config;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public TencentTTSUtil(TencentTranslateConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public String textToSpeech(String text, String lang, String voiceType) throws Exception {
        if (!isLanguageSupported(lang)) {
            throw new Exception("该语言暂不支持语音播放，目前仅支持中文和英文");
        }

        int maxLength = getMaxTextLength(lang);
        if (text.length() <= maxLength) {
            return synthesizeText(text, lang);
        }

        List<String> segments = splitText(text, maxLength);
        List<String> audioParts = new ArrayList<String>();
        for (String segment : segments) {
            String result = synthesizeText(segment, lang);
            JsonObject jsonResult = gson.fromJson(result, JsonObject.class);
            if (jsonResult.has("audio")) {
                audioParts.add(jsonResult.get("audio").getAsString());
            }
        }
        if (audioParts.isEmpty()) {
            throw new Exception("Failed to synthesize any audio segments");
        }

        JsonObject result = new JsonObject();
        result.addProperty("audio", combineAudioParts(audioParts));
        result.addProperty("format", "mp3");
        return gson.toJson(result);
    }

    private String synthesizeText(String text, String lang) throws Exception {
        String host = "tts.tencentcloudapi.com";
        String service = "tts";
        String action = "TextToVoice";
        String version = "2019-08-23";
        String algorithm = "TC3-HMAC-SHA256";
        String region = "ap-beijing";

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(Long.parseLong(timestamp) * 1000));

        int primaryLanguage = getPrimaryLanguage(lang);
        int voiceId = getVoiceId(lang);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Text", text);
        params.put("SessionId", UUID.randomUUID().toString());
        params.put("Volume", 1.0);
        params.put("Speed", 1.0);
        params.put("ProjectId", 0);
        params.put("ModelType", 1);
        params.put("VoiceType", voiceId);
        params.put("PrimaryLanguage", primaryLanguage);
        params.put("SampleRate", 16000);
        params.put("Codec", "mp3");

        String payload = gson.toJson(params);
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n" + "host:" + host + "\n";
        String signedHeaders = "content-type;host";
        String hashedRequestPayload = sha256Hex(payload);
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestPayload;
        String credentialScope = date + "/" + service + "/tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = algorithm + "\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;

        byte[] secretDate = hmacSha256(("TC3" + config.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, service);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));
        String authorization = algorithm + " " +
                "Credential=" + config.getSecretId() + "/" + credentialScope + ", " +
                "SignedHeaders=" + signedHeaders + ", " +
                "Signature=" + signature;

        RequestBody body = RequestBody.create(payload, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://" + host)
                .post(body)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Host", host)
                .addHeader("X-TC-Action", action)
                .addHeader("X-TC-Version", version)
                .addHeader("X-TC-Timestamp", timestamp)
                .addHeader("X-TC-Region", region)
                .addHeader("Authorization", authorization)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new Exception("TTS API failed: " + response.code() + " - " + responseBody);
            }

            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            if (jsonResponse == null || !jsonResponse.has("Response")) {
                throw new Exception("TTS response missing Response field");
            }
            JsonObject responseObj = jsonResponse.getAsJsonObject("Response");
            if (responseObj.has("Error")) {
                String errorCode = responseObj.getAsJsonObject("Error").get("Code").getAsString();
                String errorMsg = responseObj.getAsJsonObject("Error").get("Message").getAsString();
                throw new Exception("TTS error: " + errorCode + " - " + errorMsg);
            }
            if (!responseObj.has("Audio")) {
                throw new Exception("TTS response missing Audio field");
            }

            String audioBase64 = responseObj.get("Audio").getAsString();
            JsonObject result = new JsonObject();
            result.addProperty("audio", audioBase64);
            result.addProperty("format", "mp3");
            return gson.toJson(result);
        }
    }

    private boolean isLanguageSupported(String lang) {
        return "zh".equals(lang) || "en".equals(lang) || "eng".equals(lang) || "auto".equals(lang);
    }

    private int getMaxTextLength(String lang) {
        if ("en".equals(lang) || "eng".equals(lang)) {
            return 450;
        }
        return 140;
    }

    private List<String> splitText(String text, int maxLength) {
        List<String> segments = new ArrayList<String>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            if (end < text.length()) {
                int lastPunctuation = findLastPunctuation(text, start, end);
                if (lastPunctuation > start) {
                    end = lastPunctuation + 1;
                }
            }
            segments.add(text.substring(start, end).trim());
            start = end;
        }
        return segments;
    }

    private int findLastPunctuation(String text, int start, int end) {
        String punctuations = "。，！？；：,.!?;:";
        for (int i = end - 1; i >= start; i--) {
            if (punctuations.indexOf(text.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }

    private String combineAudioParts(List<String> audioParts) {
        return audioParts.get(0);
    }

    private int getPrimaryLanguage(String lang) {
        if ("en".equals(lang) || "eng".equals(lang)) {
            return 2;
        }
        return 1;
    }

    private int getVoiceId(String lang) {
        boolean isEnglish = "en".equals(lang) || "eng".equals(lang);
        return isEnglish ? 101050 : 101001;
    }

    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
