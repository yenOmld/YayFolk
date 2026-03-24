package com.travelate.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslateResponse {
    private int code;
    private String message;
    private String translatedText;
    private String sourceLang;
    private String targetLang;
}