package com.yayfolk.backend.controller;

import com.yayfolk.backend.agent.ExploreAgentService;
import com.yayfolk.backend.agent.result.ExploreResult;
import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.entity.ExploreConversation;
import com.yayfolk.backend.entity.ExploreMessage;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.ExploreConversationRepository;
import com.yayfolk.backend.repository.ExploreMessageRepository;
import com.yayfolk.backend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 探索资源 API（从 AIController 拆分）。
 * 新端点前缀：/api/explore
 *
 * 支持多轮对话上下文记忆：查询时自动加载历史消息传给 Agent。
 */
@RestController
@RequestMapping("/api/explore")
public class ExploreController {

    private final ExploreAgentService exploreAgentService;
    private final ExploreConversationRepository exploreConversationRepository;
    private final ExploreMessageRepository exploreMessageRepository;
    private final UserService userService;

    /** 传给 Agent 的历史消息条数上限（最近 N 条） */
    private static final int MAX_HISTORY_MESSAGES = 10;

    public ExploreController(ExploreAgentService exploreAgentService,
                              ExploreConversationRepository exploreConversationRepository,
                              ExploreMessageRepository exploreMessageRepository,
                              UserService userService) {
        this.exploreAgentService = exploreAgentService;
        this.exploreConversationRepository = exploreConversationRepository;
        this.exploreMessageRepository = exploreMessageRepository;
        this.userService = userService;
    }

    private String currentUsername(HttpServletRequest httpRequest) {
        Object usernameObj = httpRequest.getAttribute("username");
        if (usernameObj == null) {
            throw new RuntimeException("未授权，请先登录");
        }
        return usernameObj.toString();
    }

    /**
     * 探索资源查询（支持多轮对话上下文记忆）。
     */
    @PostMapping("/query")
    public ResponseDto exploreQuery(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
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

            // 保存用户消息
            ExploreMessage userMsg = new ExploreMessage();
            userMsg.setConversationId(conversationId);
            userMsg.setRole("user");
            userMsg.setContent(userInput);
            exploreMessageRepository.save(userMsg);

            // ===== 加载对话历史（上下文记忆） =====
            List<Map<String, String>> history = loadConversationHistory(conversationId);

            // 调用 Agent（传入对话历史）
            ExploreResult result = exploreAgentService.explore(userInput, history);

            // 构建响应（使用旧版 intent 名稱保持前端兼容）
            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("conversationId", conversationId);
            responseData.put("intent", toLegacyIntent(result.getIntent()));
            responseData.put("answerText", result.getAnswerText());
            responseData.put("activities", result.getActivityCards());
            responseData.put("heritages", result.getHeritageCards());
            responseData.put("posts", result.getPostCards());
            responseData.put("itinerary", result.getItineraryText());
            responseData.put("total", result.getTotal());
            // 将 metadata 扁平化到响应顶层（前端期望 days, destination 等字段）
            if (result.getMetadata() != null) {
                responseData.putAll(result.getMetadata());
            }

            String botContent = result.getAnswerText() != null
                    ? result.getAnswerText()
                    : result.buildSummary();
            String resourcesJson = exploreAgentService.serializeResources(result);

            ExploreMessage botMsg = new ExploreMessage();
            botMsg.setConversationId(conversationId);
            botMsg.setRole("assistant");
            botMsg.setContent(botContent);
            botMsg.setIntent(toLegacyIntent(result.getIntent()));
            botMsg.setResourcesJson(resourcesJson);
            exploreMessageRepository.save(botMsg);

            // 更新对话
            Optional<ExploreConversation> convOpt = exploreConversationRepository.findById(conversationId);
            if (convOpt.isPresent()) {
                ExploreConversation conv = convOpt.get();
                conv.setLastMessage(userInput.length() > 500 ? userInput.substring(0, 500) : userInput);
                conv.setLastMessageTime(new Date());
                exploreConversationRepository.save(conv);
            }

            return ResponseDto.success(responseData);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    /**
     * 加载对话历史消息，转换为 Agent 可用的格式。
     */
    private List<Map<String, String>> loadConversationHistory(Long conversationId) {
        List<Map<String, String>> history = new ArrayList<>();
        try {
            List<ExploreMessage> messages = exploreMessageRepository
                    .findByConversationIdOrderByCreateTimeAsc(conversationId);

            // 只取最近 N 条（排除刚保存的用户消息，那条是当前输入）
            int total = messages.size();
            // 最新一条是刚插入的 user 消息，排除它；再往前取 N 条
            int start = Math.max(0, total - 1 - MAX_HISTORY_MESSAGES);
            int end = total - 1; // 不包含最新一条 user 消息

            for (int i = start; i < end; i++) {
                ExploreMessage msg = messages.get(i);
                Map<String, String> entry = new HashMap<>();
                entry.put("role", msg.getRole());
                entry.put("content", msg.getContent() != null ? msg.getContent() : "");
                history.add(entry);
            }
        } catch (Exception e) {
            // 忽略加载历史失败，降级为无历史模式
        }
        return history;
    }

    @GetMapping("/conversations")
    public ResponseDto getConversations(HttpServletRequest httpRequest) {
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

    @GetMapping("/conversations/{id}/messages")
    public ResponseDto getMessages(@PathVariable Long id, HttpServletRequest httpRequest) {
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
                    map.put("resources", exploreAgentService.deserializeResources(msg.getResourcesJson()));
                }
                return map;
            }).collect(Collectors.toList());
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseDto deleteConversation(@PathVariable Long id, HttpServletRequest httpRequest) {
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

    /**
     * 将新意图枚举映射为旧格式字符串，保持前端兼容。
     */
    private String toLegacyIntent(com.yayfolk.backend.agent.intent.Intent intent) {
        switch (intent) {
            case RESOURCE_RECOMMEND: return "STRUCTURED_QUERY";
            case ITINERARY_PLANNING: return "ITINERARY_PLANNING";
            case KNOWLEDGE_QA: return "KNOWLEDGE_QA";
            case CHITCHAT: return "CHITCHAT";
            default: return "KNOWLEDGE_QA";
        }
    }
}
