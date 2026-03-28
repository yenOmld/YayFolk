package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.PublicContentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicContentService publicContentService;

    public PublicController(PublicContentService publicContentService) {
        this.publicContentService = publicContentService;
    }

    @GetMapping("/activities")
    public ResponseDto getActivities(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String city) {
        try {
            return ResponseDto.success(publicContentService.getPublicActivities(keyword, city));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/activities/{id}")
    public ResponseDto getActivityDetail(@PathVariable Long id) {
        try {
            return ResponseDto.success(publicContentService.getPublicActivityDetail(id));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/products")
    public ResponseDto getProducts(@RequestParam(required = false) String keyword) {
        try {
            return ResponseDto.success(publicContentService.getPublicProducts(keyword));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/products/{id}")
    public ResponseDto getProductDetail(@PathVariable Long id) {
        try {
            return ResponseDto.success(publicContentService.getPublicProductDetail(id));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/official")
    public ResponseDto getOfficialContents(@RequestParam(required = false) String category) {
        try {
            return ResponseDto.success(publicContentService.getOfficialContents(category));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
}
