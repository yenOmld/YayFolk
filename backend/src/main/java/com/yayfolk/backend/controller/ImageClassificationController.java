package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.ImageClassificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/classification")
public class ImageClassificationController {

    private final ImageClassificationService imageClassificationService;

    public ImageClassificationController(ImageClassificationService imageClassificationService) {
        this.imageClassificationService = imageClassificationService;
    }

    @GetMapping("/health")
    public ResponseDto health() {
        return imageClassificationService.health();
    }

    @PostMapping(value = "/predict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseDto predict(@RequestParam("image") MultipartFile image) {
        return imageClassificationService.predictTags(image);
    }
}