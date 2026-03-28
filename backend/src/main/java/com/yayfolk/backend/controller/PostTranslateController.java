package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.DiscoverService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class PostTranslateController {
    private final DiscoverService discoverService;

    public PostTranslateController(DiscoverService discoverService) {
        this.discoverService = discoverService;
    }

    @GetMapping("/post/translate")
    public ResponseDto translatePost(@RequestParam Long postId,
                                     @RequestParam(defaultValue = "zh-CN") String targetLang,
                                     HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.translatePost(username, postId, targetLang));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/comment/translate")
    public ResponseDto translateComment(@RequestParam Long commentId,
                                        @RequestParam(defaultValue = "zh-CN") String targetLang,
                                        HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.translateComment(username, commentId, targetLang));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    private String requireUsername(HttpServletRequest request) {
        Object usernameObj = request.getAttribute("username");
        if (usernameObj == null) {
            throw new RuntimeException("未授权，请先登录");
        }
        return usernameObj.toString();
    }
}
