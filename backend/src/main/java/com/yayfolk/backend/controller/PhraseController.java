package com.yayfolk.backend.controller;

import com.yayfolk.backend.entity.Phrase;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.service.PhraseService;
import com.yayfolk.backend.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/phrases")
public class PhraseController {
    @Autowired
    private PhraseService phraseService;
    
    @Autowired
    private com.yayfolk.backend.service.UserService userService;
    
    @GetMapping
    public ApiResponse<List<Phrase>> getPhrases(HttpServletRequest request) {
        Object usernameObj = request.getAttribute("username");
        if (usernameObj == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                usernameObj = auth.getName();
            }
        }
        if (usernameObj == null) {
            return new ApiResponse<>(401, "未授权，请先登录", null);
        }
        String username = usernameObj.toString();
        User user = userService.findByUsername(username);
        List<Phrase> phrases = phraseService.getPhrasesByUser(user);
        return new ApiResponse<>(200, "success", phrases);
    }
    
    @PostMapping
    public ApiResponse<Phrase> addPhrase(@RequestBody PhraseRequest requestBody, HttpServletRequest request) {
        Object usernameObj = request.getAttribute("username");
        if (usernameObj == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                usernameObj = auth.getName();
            }
        }
        if (usernameObj == null) {
            return new ApiResponse<>(401, "未授权，请先登录", null);
        }
        String username = usernameObj.toString();
        User user = userService.findByUsername(username);
        Phrase phrase = phraseService.createPhrase(user, requestBody.getText(), requestBody.getOriginalText(), requestBody.getCategory());
        return new ApiResponse<>(200, "success", phrase);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePhrase(@PathVariable Long id, HttpServletRequest request) {
        Object usernameObj = request.getAttribute("username");
        if (usernameObj == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                usernameObj = auth.getName();
            }
        }
        if (usernameObj == null) {
            return new ApiResponse<>(401, "未授权，请先登录", null);
        }
        String username = usernameObj.toString();
        User user = userService.findByUsername(username);
        phraseService.deletePhrase(id, user);
        return new ApiResponse<>(200, "success", null);
    }
    
    // 请求参数类
    public static class PhraseRequest {
        private String text;
        private String originalText;
        private String category;
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public String getOriginalText() {
            return originalText;
        }
        
        public void setOriginalText(String originalText) {
            this.originalText = originalText;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
    }
}
