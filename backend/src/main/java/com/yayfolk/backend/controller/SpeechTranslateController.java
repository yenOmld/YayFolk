package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ApiResponse;
import com.yayfolk.backend.dto.SpeechTranslateResult;
import com.yayfolk.backend.service.SpeechTranslateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/translate/speech")
@RequiredArgsConstructor
public class SpeechTranslateController {

    private final SpeechTranslateService speechTranslateService;

    @PostMapping(value = "/translate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SpeechTranslateResult> translateSpeech(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "from", defaultValue = "en") String from,
            @RequestParam(value = "to", defaultValue = "zh") String to,
            @RequestParam(value = "format", defaultValue = "wav") String format) {
        try {
            if (audio.isEmpty()) {
                return ApiResponse.error(400, "Audio file is empty");
            }
            byte[] audioData = audio.getBytes();
            SpeechTranslateResult result = speechTranslateService.translateSpeech(audioData, from, to, format);
            return ApiResponse.success("Translation completed", result);
        } catch (IOException e) {
            log.error("Speech translation failed", e);
            return ApiResponse.error("Translation failed: " + e.getMessage());
        }
    }

    @PostMapping("/text-to-speech")
    public ApiResponse<Map<String, String>> textToSpeech(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            String lang = request.containsKey("lang") ? request.get("lang") : "zh";
            String voiceType = request.get("voiceType");
            if (text == null || text.trim().isEmpty()) {
                return ApiResponse.error(400, "Text is required");
            }
            String audioUrl = speechTranslateService.textToSpeech(text, lang, voiceType);
            if (audioUrl == null) {
                return ApiResponse.error("TTS failed: unable to generate audio");
            }
            Map<String, String> data = new HashMap<String, String>();
            data.put("audioUrl", audioUrl);
            return ApiResponse.success("TTS completed", data);
        } catch (Exception e) {
            log.error("TTS failed", e);
            return ApiResponse.error("TTS failed: " + e.getMessage());
        }
    }
}
