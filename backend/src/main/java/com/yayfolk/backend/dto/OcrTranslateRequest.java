package com.yayfolk.backend.dto;

import lombok.Data;

@Data
public class OcrTranslateRequest {
    private String imageBase64;
    private String sourceLang;
    private String targetLang;
}
