package com.travelate.backend.controller;

import com.travelate.backend.dto.ResponseDto;
import com.travelate.backend.service.MessageService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversations")
    public ResponseDto getConversations(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(messageService.getConversationList(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/conversations")
    public ResponseDto createConversation(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            Long otherUserId = Long.parseLong(payload.get("otherUserId").toString());
            return ResponseDto.success(messageService.getOrCreateConversation(username, otherUserId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/conversations/{id}")
    public ResponseDto getMessages(@PathVariable("id") Long conversationId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(messageService.getMessages(username, conversationId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/conversations/{id}")
    public ResponseDto sendMessage(@PathVariable("id") Long conversationId,
                                   @RequestBody Map<String, Object> payload,
                                   HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            String content = payload.get("content").toString();
            return ResponseDto.success(messageService.sendMessage(username, conversationId, content));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseDto markAsRead(@PathVariable("id") Long conversationId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            messageService.markAsRead(username, conversationId);
            return ResponseDto.success("已标记为已读");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/notifications/{type}")
    public ResponseDto getNotifications(@PathVariable("type") String type, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(messageService.getNotifications(username, type));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/notifications/{type}/read")
    public ResponseDto markNotificationsAsRead(@PathVariable("type") String type, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            messageService.markNotificationsAsRead(username, type);
            return ResponseDto.success("已标记为已读");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    public ResponseDto getUnreadCount(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(messageService.getTotalUnreadCount(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/message/translate")
    public ResponseDto translateMessage(@RequestParam Long messageId,
                                        @RequestParam(defaultValue = "zh-CN") String targetLang,
                                        HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(messageService.translateMessage(username, messageId, targetLang));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseDto deleteConversation(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            messageService.deleteConversation(username, id);
            return ResponseDto.success("会话已删除");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/notifications/{type}")
    public ResponseDto clearNotifications(@PathVariable("type") String type, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            messageService.clearNotifications(username, type);
            return ResponseDto.success("通知已清除");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/messages/{id}")
    public ResponseDto deleteMessage(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            messageService.deleteMessage(username, id);
            return ResponseDto.success("消息已删除");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/messages/{id}/recall")
    public ResponseDto recallMessage(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(messageService.recallMessage(username, id));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    private String requireUsername(HttpServletRequest request) {
        Object usernameObj = request.getAttribute("username");
        if (usernameObj == null) {
            throw new RuntimeException("未登录");
        }
        return usernameObj.toString();
    }
}
