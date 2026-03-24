package com.travelate.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechTranslateResult {
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private String audioUrl;
}
