package com.yayfolk.backend.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yayfolk.backend.config.BaiduTranslateConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduTranslateUtil {

    private final BaiduTranslateConfig config;
    private final Gson gson = new Gson();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private String generateSign(String query, String salt) {
        String signStr = config.getAppId() + query + salt + config.getSecretKey();
        return DigestUtils.md5Hex(signStr);
    }

    private String generateHmacSha256Sign(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256 signature", e);
        }
    }

    public String translateText(String query, String from, String to) throws IOException {
        String salt = String.valueOf(System.currentTimeMillis());
        String sign = generateSign(query, salt);

        HttpUrl url = HttpUrl.parse(config.getTextTranslateUrl()).newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("from", from)
                .addQueryParameter("to", to)
                .addQueryParameter("appid", config.getAppId())
                .addQueryParameter("salt", salt)
                .addQueryParameter("sign", sign)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Translation API failed: " + response.code());
            }
            String responseBody = response.body().string();
            log.info("Text translation response: {}", responseBody);
            return responseBody;
        }
    }

    public String translateImage(MultipartFile imageFile, String from, String to) throws IOException {
        byte[] imageBytes = imageFile.getBytes();
        String imageMd5 = DigestUtils.md5Hex(imageBytes);
        String salt = String.valueOf(System.currentTimeMillis());
        String cuid = "APICUID";
        String mac = "mac";

        String signStr = config.getAppId() + imageMd5 + salt + cuid + mac + config.getSecretKey();
        String sign = DigestUtils.md5Hex(signStr);

        HttpUrl url = HttpUrl.parse(config.getImageTranslateUrl()).newBuilder()
                .addQueryParameter("from", from)
                .addQueryParameter("to", to)
                .addQueryParameter("appid", config.getAppId())
                .addQueryParameter("salt", salt)
                .addQueryParameter("sign", sign)
                .addQueryParameter("cuid", cuid)
                .addQueryParameter("mac", mac)
                .addQueryParameter("version", "3")
                .build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.getOriginalFilename(), RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.body().string();
        }
    }

    public String translateSpeech(byte[] audioData, String from, String to, String format) throws IOException {
        String voiceBase64 = Base64.getEncoder().encodeToString(audioData);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        String signMessage = config.getAppId() + timestamp + voiceBase64;
        String sign = generateHmacSha256Sign(signMessage, config.getSecretKey());

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("from", from);
        bodyJson.addProperty("to", to);
        bodyJson.addProperty("format", format);
        bodyJson.addProperty("voice", voiceBase64);

        String jsonBody = gson.toJson(bodyJson);

        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(config.getSpeechTranslateUrl())
                .header("Content-Type", "application/json")
                .header("X-Appid", config.getAppId())
                .header("X-Timestamp", timestamp)
                .header("X-Sign", sign)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            log.info("Speech translation response: {}", responseBody);
            return responseBody;
        }
    }

    public String textToSpeech(String text, String lang, String speed, String pitch) throws IOException {
        String salt = String.valueOf(System.currentTimeMillis());
        String cuid = "APICUID";
        String signStr = config.getAppId() + text + salt + config.getSecretKey();
        String sign = DigestUtils.md5Hex(signStr);

        RequestBody formBody = new FormBody.Builder()
                .add("tex", text)
                .add("lan", lang)
                .add("spd", speed != null ? speed : "5")
                .add("pit", pitch != null ? pitch : "5")
                .add("appid", config.getAppId())
                .add("salt", salt)
                .add("sign", sign)
                .add("cuid", cuid)
                .build();

        Request request = new Request.Builder()
                .url(config.getTtsUrl())
                .post(formBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String contentType = response.header("Content-Type");
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No body";
                throw new IOException("TTS API failed: " + response.code() + ", " + errorBody);
            }
            byte[] audioBytes = response.body().bytes();
            if (contentType != null && contentType.contains("application/json")) {
                String jsonBody = new String(audioBytes, StandardCharsets.UTF_8);
                throw new IOException("TTS returned JSON: " + jsonBody);
            }
            if (audioBytes.length == 0) {
                throw new IOException("TTS returned empty audio");
            }
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);
            JsonObject result = new JsonObject();
            result.addProperty("audio", audioBase64);
            result.addProperty("format", "mp3");
            return gson.toJson(result);
        }
    }
}
