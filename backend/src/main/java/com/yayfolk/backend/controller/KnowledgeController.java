package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.entity.KnowledgeConversation;
import com.yayfolk.backend.entity.KnowledgeMessage;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.KnowledgeConversationRepository;
import com.yayfolk.backend.repository.KnowledgeMessageRepository;
import com.yayfolk.backend.service.AICustomerService;
import com.yayfolk.backend.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeConversationRepository knowledgeConversationRepository;
    private final KnowledgeMessageRepository knowledgeMessageRepository;
    private final AICustomerService aiCustomerService;
    private final UserService userService;

    public KnowledgeController(KnowledgeConversationRepository knowledgeConversationRepository,
                               KnowledgeMessageRepository knowledgeMessageRepository,
                               AICustomerService aiCustomerService,
                               UserService userService) {
        this.knowledgeConversationRepository = knowledgeConversationRepository;
        this.knowledgeMessageRepository = knowledgeMessageRepository;
        this.aiCustomerService = aiCustomerService;
        this.userService = userService;
    }

    @GetMapping("/conversations")
    public ResponseDto getConversations(HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            User user = userService.findByUsername(username);
            List<KnowledgeConversation> conversations = knowledgeConversationRepository
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

    @PostMapping("/conversations")
    public ResponseDto createConversation(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            User user = userService.findByUsername(username);
            Long userId = user.getId();

            KnowledgeConversation conversation = new KnowledgeConversation();
            conversation.setUserId(userId);
            String userInput = request.get("userInput") != null ? request.get("userInput").toString() : "";
            conversation.setTitle(userInput.length() > 50 ? userInput.substring(0, 50) : userInput);
            conversation.setLastMessageTime(new Date());
            conversation = knowledgeConversationRepository.save(conversation);

            Map<String, Object> result = new HashMap<>();
            result.put("id", conversation.getId());
            result.put("title", conversation.getTitle());
            result.put("createTime", conversation.getCreateTime());
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseDto getMessages(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            User user = userService.findByUsername(username);

            Optional<KnowledgeConversation> convOpt = knowledgeConversationRepository.findById(id);
            if (!convOpt.isPresent() || !convOpt.get().getUserId().equals(user.getId())) {
                return ResponseDto.error(403, "无权访问此对话");
            }

            List<KnowledgeMessage> messages = knowledgeMessageRepository.findByConversationIdOrderByCreateTimeAsc(id);
            List<Map<String, Object>> result = messages.stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.getId());
                map.put("isSelf", msg.getIsSelf() == 1);
                map.put("content", msg.getContent());
                map.put("createTime", msg.getCreateTime());
                return map;
            }).collect(Collectors.toList());
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/conversations/{id}/message")
    public ResponseDto sendMessage(@PathVariable Long id,
                                   @RequestBody Map<String, Object> request,
                                   HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            User user = userService.findByUsername(username);
            Long userId = user.getId();

            Optional<KnowledgeConversation> convOpt = knowledgeConversationRepository.findById(id);
            if (!convOpt.isPresent() || !convOpt.get().getUserId().equals(userId)) {
                return ResponseDto.error(403, "无权在此对话中发送消息");
            }

            String userInput = request.get("content") != null ? request.get("content").toString().trim() : "";
            if (userInput.isEmpty()) {
                return ResponseDto.error(400, "消息内容不能为空");
            }

            KnowledgeMessage userMsg = new KnowledgeMessage();
            userMsg.setConversationId(id);
            userMsg.setIsSelf(1);
            userMsg.setContent(userInput);
            knowledgeMessageRepository.save(userMsg);

            List<KnowledgeMessage> conversationHistory = knowledgeMessageRepository.findByConversationIdOrderByCreateTimeAsc(id);
            List<com.yayfolk.backend.entity.Message> messageHistory = new ArrayList<>();
            for (KnowledgeMessage km : conversationHistory) {
                com.yayfolk.backend.entity.Message msg = new com.yayfolk.backend.entity.Message();
                msg.setSenderId(km.getIsSelf() == 1 ? userId : 1L);
                msg.setContent(km.getContent());
                messageHistory.add(msg);
            }

            String aiResponse = aiCustomerService.generateResponse(messageHistory, userInput);

            KnowledgeMessage botMsg = new KnowledgeMessage();
            botMsg.setConversationId(id);
            botMsg.setIsSelf(0);
            botMsg.setContent(aiResponse);
            knowledgeMessageRepository.save(botMsg);

            Optional<KnowledgeConversation> conv = knowledgeConversationRepository.findById(id);
            if (conv.isPresent()) {
                KnowledgeConversation conversation = conv.get();
                conversation.setLastMessage(userInput.length() > 500 ? userInput.substring(0, 500) : userInput);
                conversation.setLastMessageTime(new Date());
                knowledgeConversationRepository.save(conversation);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("content", aiResponse);
            result.put("conversationId", id);
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping(value = "/conversations/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable Long id,
                                    @RequestParam("content") String content,
                                    @RequestParam("token") String token,
                                    HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(300000L);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                String username = null;
                try {
                    username = parseUsernameFromToken(token);
                    if (username == null) {
                        throw new RuntimeException("Token无效");
                    }
                } catch (Exception e) {
                    emitter.send(SseEmitter.event().data("身份验证失败，请重新登录。"));
                    emitter.complete();
                    executor.shutdown();
                    return;
                }

                User user = userService.findByUsername(username);
                Long userId = user.getId();

                Optional<KnowledgeConversation> convOpt = knowledgeConversationRepository.findById(id);
                if (!convOpt.isPresent() || !convOpt.get().getUserId().equals(userId)) {
                    emitter.send(SseEmitter.event().data("无权在此对话中发送消息"));
                    emitter.complete();
                    executor.shutdown();
                    return;
                }

                String userInput = content != null ? content.trim() : "";
                if (userInput.isEmpty()) {
                    emitter.send(SseEmitter.event().data("消息内容不能为空"));
                    emitter.complete();
                    executor.shutdown();
                    return;
                }

                KnowledgeMessage userMsg = new KnowledgeMessage();
                userMsg.setConversationId(id);
                userMsg.setIsSelf(1);
                userMsg.setContent(userInput);
                knowledgeMessageRepository.save(userMsg);

                List<KnowledgeMessage> conversationHistory = knowledgeMessageRepository.findByConversationIdOrderByCreateTimeAsc(id);
                List<com.yayfolk.backend.entity.Message> messageHistory = new ArrayList<>();
                for (KnowledgeMessage km : conversationHistory) {
                    com.yayfolk.backend.entity.Message msg = new com.yayfolk.backend.entity.Message();
                    msg.setSenderId(km.getIsSelf() == 1 ? userId : 1L);
                    msg.setContent(km.getContent());
                    messageHistory.add(msg);
                }

                StringBuilder aiResponseBuilder = new StringBuilder();

                Flux<String> responseFlux = aiCustomerService.streamGenerateResponse(messageHistory, userInput);

                responseFlux.subscribe(
                    data -> {
                        try {
                            if (data != null && !data.isEmpty()) {
                                aiResponseBuilder.append(data);
                                emitter.send(SseEmitter.event().data(data));
                            }
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        try {
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
                            String aiResponse = aiResponseBuilder.toString();
                            if (!aiResponse.isEmpty()) {
                                KnowledgeMessage botMsg = new KnowledgeMessage();
                                botMsg.setConversationId(id);
                                botMsg.setIsSelf(0);
                                botMsg.setContent(aiResponse);
                                knowledgeMessageRepository.save(botMsg);

                                Optional<KnowledgeConversation> conv = knowledgeConversationRepository.findById(id);
                                if (conv.isPresent()) {
                                    KnowledgeConversation conversation = conv.get();
                                    conversation.setLastMessage(userInput.length() > 500 ? userInput.substring(0, 500) : userInput);
                                    conversation.setLastMessageTime(new Date());
                                    knowledgeConversationRepository.save(conversation);
                                }
                            }
                            emitter.send(SseEmitter.event().data("[DONE]"));
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

    private String parseUsernameFromToken(String token) {
        try {
            if (!com.yayfolk.backend.utils.JwtUtil.validateToken(token)) {
                return null;
            }
            return com.yayfolk.backend.utils.JwtUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseDto deleteConversation(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            User user = userService.findByUsername(username);

            Optional<KnowledgeConversation> convOpt = knowledgeConversationRepository.findById(id);
            if (!convOpt.isPresent() || !convOpt.get().getUserId().equals(user.getId())) {
                return ResponseDto.error(403, "无权删除此对话");
            }

            List<KnowledgeMessage> messages = knowledgeMessageRepository.findByConversationIdOrderByCreateTimeAsc(id);
            knowledgeMessageRepository.deleteAll(messages);
            knowledgeConversationRepository.deleteById(id);
            return ResponseDto.success("删除成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    private String currentUsername(HttpServletRequest httpRequest) {
        Object usernameObj = httpRequest.getAttribute("username");
        if (usernameObj == null) {
            throw new RuntimeException("未授权，请先登录");
        }
        return usernameObj.toString();
    }
}
