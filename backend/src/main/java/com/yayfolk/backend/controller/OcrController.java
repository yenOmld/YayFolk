package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.OcrTranslateRequest;
import com.yayfolk.backend.dto.OcrTranslateResponse;
import com.yayfolk.backend.service.OcrTranslateService;
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
