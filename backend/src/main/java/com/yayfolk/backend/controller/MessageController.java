package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.entity.Message;
import com.yayfolk.backend.service.MessageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.yayfolk.backend.utils.JwtUtil;

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

    @PostMapping("/customer-service")
    public ResponseDto createCustomerServiceConversation(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(messageService.getOrCreateCustomerServiceConversation(username));
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

    @GetMapping(value = "/conversations/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable("id") Long conversationId,
                                   @RequestParam("content") String content,
                                   @RequestParam("token") String token,
                                   HttpServletRequest request) {
        // 设置SSE超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300000L);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        executor.execute(() -> {
            try {
                // 从token中获取用户信息
                String username = null;
                try {
                    // 解析token获取用户名
                    username = parseUsernameFromToken(token);
                    if (username == null) {
                        throw new RuntimeException("Token无效");
                    }
                    request.setAttribute("username", username);
                } catch (Exception e) {
                    // 如果token验证失败，发送错误信息并关闭连接
                    emitter.send(SseEmitter.event().data("身份验证失败，请重新登录。"));
                    emitter.complete();
                    executor.shutdown();
                    return;
                }
                
                // 获取用户ID
                Long userId = messageService.findUserId(username);
                
                // 流式生成AI回复
                Flux<String> responseFlux = messageService.streamGenerateAIResponse(conversationId, userId, content);
                
                // 用于累积AI回复内容
                StringBuilder aiResponseBuilder = new StringBuilder();
                
                // 订阅Flux，将每个数据块发送给前端（使用标准SSE格式）
                responseFlux.subscribe(
                    data -> {
                        try {
                            if (data != null && !data.isEmpty()) {
                                System.out.println("发送数据到前端: " + data);
                                aiResponseBuilder.append(data);
                                // 发送数据，SseEmitter会自动处理SSE格式
                                emitter.send(SseEmitter.event().data(data));
                                System.out.println("数据发送完成");
                            }
                        } catch (IOException e) {
                            System.err.println("发送数据失败: " + e.getMessage());
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        try {
                            System.err.println("Flux 发生错误: " + error.getMessage());
                            emitter.send(SseEmitter.event().data("抱歉，AI客服暂时无法回复，请稍后再试。"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            emitter.complete();
                            executor.shutdown();
                        }
                    },
                    () -> {
                        try {
                            System.out.println("Flux 完成，发送结束标记");
                            // 完成后发送结束标记
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            
                            // 保存AI回复到数据库
                            String aiResponse = aiResponseBuilder.toString();
                            if (!aiResponse.isEmpty()) {
                                // 使用messageService保存AI回复
                                messageService.saveAIMessage(conversationId, userId, aiResponse);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            emitter.complete();
                            executor.shutdown();
                        }
                    }
                );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("抱歉，AI客服暂时无法回复，请稍后再试。"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    emitter.complete();
                    executor.shutdown();
                }
            }
        });
        
        return emitter;
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
            return ResponseDto.success("通知已清空");
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

    @GetMapping("/conversations/{id}/service-mode")
    public ResponseDto getServiceMode(@PathVariable("id") Long conversationId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(messageService.getServiceMode(username, conversationId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/conversations/{id}/close-human-service")
    public ResponseDto closeHumanService(@PathVariable("id") Long conversationId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            messageService.closeHumanService(username, conversationId);
            return ResponseDto.success("已切换回智能客服");
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

    private String parseUsernameFromToken(String token) {
        try {
            // 验证token是否有效
            if (!JwtUtil.validateToken(token)) {
                return null;
            }
            
            // 从token中获取用户名
            return JwtUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}