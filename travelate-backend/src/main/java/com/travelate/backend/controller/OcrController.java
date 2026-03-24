package com.travelate.backend.controller;

import com.travelate.backend.dto.OcrTranslateRequest;
import com.travelate.backend.dto.OcrTranslateResponse;
import com.travelate.backend.service.OcrTranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ocr")
@Slf4j
@CrossOrigin(origins = "*")
public class OcrController {
    
    @Autowired
    private OcrTranslateService ocrTranslateService;
    
    @PostMapping("/translate")
    public OcrTranslateResponse ocrTranslate(@RequestBody OcrTranslateRequest request) {
        log.info("Received OCR translate request");
        return ocrTranslateService.ocrTranslate(request);
    }
}