package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.TranslateRequest;
import com.yayfolk.backend.dto.TranslateResponse;
import com.yayfolk.backend.service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translate")
@Slf4j
@CrossOrigin(origins = "*")
public class TranslateController {
    
    @Autowired
    private TranslateService translateService;
    
    @PostMapping
    public TranslateResponse translate(@RequestBody TranslateRequest request) {
        log.info("Received translate request: {} -> {}", request.getSourceLang(), request.getTargetLang());
        return translateService.translate(request);
    }
}
