package com.yayfolk.backend.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yayfolk.backend.config.TencentTranslateConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TencentTranslateUtil {

    private final TencentTranslateConfig config;
    private final Gson gson = new Gson();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public String translateText(String query, String from, String to) throws IOException {
        try {
            String service = "tmt";
            String host = config.getEndpoint();
            String region = config.getRegion();
            String action = "TextTranslate";
            String version = "2018-03-21";
            String algorithm = "TC3-HMAC-SHA256";
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String date = sdf.format(new Date(Long.parseLong(timestamp) * 1000L));

            Map<String, Object> params = new TreeMap<String, Object>();
            params.put("SourceText", query);
            params.put("Source", convertLanguageCode(from));
            params.put("Target", convertLanguageCode(to));
            params.put("ProjectId", 0);

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
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Host", host)
                    .header("X-TC-Action", action)
                    .header("X-TC-Version", version)
                    .header("X-TC-Timestamp", timestamp)
                    .header("X-TC-Region", region)
                    .header("Authorization", authorization)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                if (jsonResponse.has("Response")) {
                    JsonObject responseObj = jsonResponse.getAsJsonObject("Response");
                    if (responseObj.has("Error")) {
                        JsonObject error = responseObj.getAsJsonObject("Error");
                        String errorMsg = error.has("Message") ? error.get("Message").getAsString() : "Unknown error";
                        throw new IOException("Tencent API Error: " + errorMsg);
                    }
                    if (responseObj.has("TargetText")) {
                        String translatedText = responseObj.get("TargetText").getAsString();
                        JsonObject result = new JsonObject();
                        result.addProperty("from", from);
                        result.addProperty("to", to);
                        JsonObject transResult = new JsonObject();
                        transResult.addProperty("src", query);
                        transResult.addProperty("dst", translatedText);
                        result.add("trans_result", gson.toJsonTree(Collections.singletonList(transResult)));
                        return gson.toJson(result);
                    }
                }
                throw new IOException("Invalid response from Tencent API");
            }
        } catch (Exception e) {
            log.error("Tencent translate error", e);
            throw new IOException("Tencent translate failed: " + e.getMessage(), e);
        }
    }

    private String convertLanguageCode(String code) {
        Map<String, String> codeMap = new HashMap<String, String>();
        codeMap.put("zh", "zh");
        codeMap.put("en", "en");
        codeMap.put("jp", "ja");
        codeMap.put("kor", "ko");
        codeMap.put("fra", "fr");
        codeMap.put("de", "de");
        codeMap.put("spa", "es");
        codeMap.put("ru", "ru");
        codeMap.put("pt", "pt");
        codeMap.put("it", "it");
        codeMap.put("auto", "auto");
        return codeMap.getOrDefault(code, code);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] hmacSha256(byte[] key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
