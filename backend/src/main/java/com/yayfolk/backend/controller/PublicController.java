package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.PublicContentService;
import com.yayfolk.backend.service.UnbanApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicContentService publicContentService;
    private final UnbanApplicationService unbanApplicationService;

    public PublicController(PublicContentService publicContentService,
                            UnbanApplicationService unbanApplicationService) {
        this.publicContentService = publicContentService;
        this.unbanApplicationService = unbanApplicationService;
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

    @GetMapping("/official/homepage")
    public ResponseDto getHomepageOfficialContents() {
        try {
            return ResponseDto.success(publicContentService.getHomepageOfficialContents());
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
    @PostMapping("/unban-applications")
    public ResponseDto submitUnbanApplication(@RequestBody Map<String, Object> data) {
        try {
            String account = data.get("account") != null ? data.get("account").toString() : null;
            String reason = data.get("reason") != null ? data.get("reason").toString() : null;
            return ResponseDto.success(unbanApplicationService.submitApplication(account, reason));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/unban-applications/latest")
    public ResponseDto getLatestUnbanApplication(@RequestParam String account) {
        try {
            return ResponseDto.success(unbanApplicationService.getLatestApplication(account));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
}


