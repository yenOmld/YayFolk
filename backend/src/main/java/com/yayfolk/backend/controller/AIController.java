package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.entity.ExploreConversation;
import com.yayfolk.backend.entity.ExploreMessage;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.ExploreConversationRepository;
import com.yayfolk.backend.repository.ExploreMessageRepository;

import com.yayfolk.backend.service.AIResourceService;
import com.yayfolk.backend.service.DoubaoHeritagePosterService;
import com.yayfolk.backend.service.UserService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//  AI辅助生成：豆包API（Doubao-Seedream-5.0-lite），2025-04-24
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIResourceService aiResourceService;
    private final ExploreConversationRepository exploreConversationRepository;
    private final ExploreMessageRepository exploreMessageRepository;
    private final UserService userService;
    private final DoubaoHeritagePosterService doubaoHeritagePosterService;

    public AIController(AIResourceService aiResourceService,
                        ExploreConversationRepository exploreConversationRepository,
                        ExploreMessageRepository exploreMessageRepository,
                        UserService userService,
                        DoubaoHeritagePosterService doubaoHeritagePosterService) {
        this.aiResourceService = aiResourceService;
        this.exploreConversationRepository = exploreConversationRepository;
        this.exploreMessageRepository = exploreMessageRepository;
        this.userService = userService;
        this.doubaoHeritagePosterService = doubaoHeritagePosterService;
    }



    private String currentUsername(HttpServletRequest httpRequest) {
        Object usernameObj = httpRequest.getAttribute("username");
        if (usernameObj == null) {
            throw new RuntimeException("未授权，请先登录");
        }
        return usernameObj.toString();
    }

    @PostMapping("/explore-resources")
    public ResponseDto exploreResources(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            User user = userService.findByUsername(username);
            Long userId = user.getId();

            Long conversationId = request.get("conversationId") != null
                    ? Long.parseLong(request.get("conversationId").toString())
                    : null;

            if (conversationId == null) {
                ExploreConversation conversation = new ExploreConversation();
                conversation.setUserId(userId);
                String userInput = request.get("userInput") != null ? request.get("userInput").toString() : "";
                conversation.setTitle(userInput.length() > 50 ? userInput.substring(0, 50) : userInput);
                conversation.setLastMessageTime(new Date());
                conversation = exploreConversationRepository.save(conversation);
                conversationId = conversation.getId();
            }

            String userInput = request.get("userInput") == null ? "" : request.get("userInput").toString().trim();

            ExploreMessage userMsg = new ExploreMessage();
            userMsg.setConversationId(conversationId);
            userMsg.setRole("user");
            userMsg.setContent(userInput);
            exploreMessageRepository.save(userMsg);

            Map<String, Object> result = aiResourceService.exploreResources(request);
            result.put("conversationId", conversationId);

            String intent = result.get("intent") != null ? result.get("intent").toString() : "";
            String botContent = aiResourceService.buildSummaryText(result);

            ExploreMessage botMsg = new ExploreMessage();
            botMsg.setConversationId(conversationId);
            botMsg.setRole("assistant");
            botMsg.setContent(botContent);
            botMsg.setIntent(intent);
            botMsg.setResourcesJson(aiResourceService.serializeResources(result));
            exploreMessageRepository.save(botMsg);

            Optional<ExploreConversation> convOpt = exploreConversationRepository.findById(conversationId);
            if (convOpt.isPresent()) {
                ExploreConversation conv = convOpt.get();
                conv.setLastMessage(userInput.length() > 500 ? userInput.substring(0, 500) : userInput);
                conv.setLastMessageTime(new Date());
                exploreConversationRepository.save(conv);
            }

            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/explore-conversations")
    public ResponseDto getExploreConversations(HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            User user = userService.findByUsername(username);
            List<ExploreConversation> conversations = exploreConversationRepository
                    .findByUserIdOrderByLastMessageTimeDesc(user.getId());
            List<Map<String, Object>> result = conversations.stream().map(conv -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", conv.getId());
                map.put("title", conv.getTitle());
                map.put("lastMessage", conv.getLastMessage());
                map.put("lastMessageTime", conv.getLastMessageTime());
                map.put("createTime", conv.getCreateTime());
                return map;
            }).collect(Collectors.toList());
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/explore-conversations/{id}/messages")
    public ResponseDto getExploreMessages(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            User user = userService.findByUsername(username);

            Optional<ExploreConversation> convOpt = exploreConversationRepository.findById(id);
            if (!convOpt.isPresent() || !convOpt.get().getUserId().equals(user.getId())) {
                return ResponseDto.error(403, "无权访问此对话");
            }

            List<ExploreMessage> messages = exploreMessageRepository.findByConversationIdOrderByCreateTimeAsc(id);
            List<Map<String, Object>> result = messages.stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.getId());
                map.put("role", msg.getRole());
                map.put("content", msg.getContent());
                map.put("intent", msg.getIntent());
                map.put("createTime", msg.getCreateTime());
                if (msg.getResourcesJson() != null) {
                    map.put("resources", aiResourceService.deserializeResources(msg.getResourcesJson()));
                }
                return map;
            }).collect(Collectors.toList());
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/explore-conversations/{id}")
    public ResponseDto deleteExploreConversation(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            User user = userService.findByUsername(username);

            Optional<ExploreConversation> convOpt = exploreConversationRepository.findById(id);
            if (!convOpt.isPresent() || !convOpt.get().getUserId().equals(user.getId())) {
                return ResponseDto.error(403, "无权删除此对话");
            }

            List<ExploreMessage> messages = exploreMessageRepository.findByConversationIdOrderByCreateTimeAsc(id);
            exploreMessageRepository.deleteAll(messages);
            exploreConversationRepository.deleteById(id);
            return ResponseDto.success("删除成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/rebuild-index")
    public ResponseDto rebuildIndex() {
        try {
            aiResourceService.buildVectorIndex();
            return ResponseDto.success("向量索引重建成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/heritage-poster")
    public ResponseDto generateAiHeritagePoster(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = doubaoHeritagePosterService.generatePoster(request);
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    /**
     * 代理下载远程图片，解决前端跨域问题
     */
    @GetMapping("/proxy-image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam("url") String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // 只允许下载豆包API生成的图片
            if (!imageUrl.contains("volces.com") && !imageUrl.contains("doubao")) {
                return ResponseEntity.badRequest().build();
            }
            
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return ResponseEntity.status(responseCode).build();
            }
            
            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = "image/jpeg";
            }
            
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                byte[] imageBytes = outputStream.toByteArray();
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentLength(imageBytes.length);
                headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
                
                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
