package com.travelate.backend.dto;

import lombok.Data;

@Data
public class OcrTranslateRequest {
    private String imageBase64;
    private String sourceLang;
    private String targetLang;
}