package com.yayfolk.backend.service;

import com.yayfolk.backend.ai.client.DeepSeekClient;
import com.yayfolk.backend.ai.client.DeepSeekStreamClient;
import com.yayfolk.backend.entity.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// AI辅助生成：DeepSeek API（deepseek-v4-flash），2025-04-26
@Service
public class AICustomerService {

    @Value("${HTTP_PROXY_HOST:}")
    private String httpProxyHost;

    @Value("${HTTP_PROXY_PORT:}")
    private String httpProxyPort;

    private final DeepSeekClient deepSeekClient;
    private final DeepSeekStreamClient deepSeekStreamClient;

    public AICustomerService(DeepSeekClient deepSeekClient, DeepSeekStreamClient deepSeekStreamClient) {
        this.deepSeekClient = deepSeekClient;
        this.deepSeekStreamClient = deepSeekStreamClient;

        // 代理配置
        if (httpProxyHost != null && !httpProxyHost.isEmpty() && httpProxyPort != null && !httpProxyPort.isEmpty()) {
            System.setProperty("http.proxyHost", httpProxyHost);
            System.setProperty("http.proxyPort", httpProxyPort);
            System.setProperty("https.proxyHost", httpProxyHost);
            System.setProperty("https.proxyPort", httpProxyPort);
        }
    }

    // 非流式响应
    public String generateResponse(List<Message> conversationHistory, String latestMessage) {
        com.yayfolk.backend.ai.model.ChatRequest request =
                new com.yayfolk.backend.ai.model.ChatRequest()
                        .withMaxTokens(200)
                        .withTemperature(0.7);

        // 系统提示
        request.addMessage(com.yayfolk.backend.ai.model.ChatMessage.system(buildSystemPrompt()));

        // 历史消息（最多5条）
        int maxHistoryMessages = 5;
        int startIndex = Math.max(0, conversationHistory.size() - maxHistoryMessages);
        for (int i = startIndex; i < conversationHistory.size(); i++) {
            Message message = conversationHistory.get(i);
            String role = message.getSenderId() == 1L ? "assistant" : "user";
            request.addMessage(new com.yayfolk.backend.ai.model.ChatMessage(role, message.getContent()));
        }

        // 当前用户消息
        request.addMessage(com.yayfolk.backend.ai.model.ChatMessage.user(latestMessage));

        String result = deepSeekClient.chat(request);
        return result != null ? result : "抱歉，AI客服暂时无法回复，请稍后再试。";
    }

    // 流式响应
    public Flux<String> streamGenerateResponse(List<Message> conversationHistory, String latestMessage) {
        List<Map<String, Object>> messages = buildMessages(conversationHistory, latestMessage);
        return deepSeekStreamClient.streamChat(messages);
    }

    private String buildSystemPrompt() {
        return "你是一个智能客服助手，名为Yaya。你的任务是友好、专业地回答用户有关中国传统非物质文化遗产（非遗）的问题，提供帮助和支持。\n\n" +
                "重要要求：\n" +
                "1. 请使用自然流畅的中文回复，避免使用Markdown格式（如**粗体**、--分割线、#标题等）\n" +
                "2. 请确保回复内容简洁明了，直接回答问题，不超过300字。\n" +
                "3. 只提供必要的信息，避免冗长的解释和多余的内容。\n" +
                "4. 你只能回答与中国传统非遗相关的问题，包括非遗的历史、特点、传承、保护等方面。\n" +
                "5. 对于偏离中国非遗主题的问题，请礼貌地提示用户：'和Yaya一起来探索博大精深的中国传统非物质文化遗产吧！'\n" +
                "6. 确保你的回答始终与中国非遗相关，不要涉及其他无关内容。";
    }



    // 构建消息列表（系统提示 + 历史 + 当前消息）
    private List<Map<String, Object>> buildMessages(List<Message> conversationHistory, String latestMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 系统提示
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", buildSystemPrompt());
        messages.add(systemMessage);

        // 历史消息（最多 5 条）
        int maxHistoryMessages = 5;
        int startIndex = Math.max(0, conversationHistory.size() - maxHistoryMessages);
        for (int i = startIndex; i < conversationHistory.size(); i++) {
            Message message = conversationHistory.get(i);
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", message.getSenderId() == 1L ? "assistant" : "user");
            msg.put("content", message.getContent());
            messages.add(msg);
        }

        // 当前用户消息
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", latestMessage);
        messages.add(userMessage);

        return messages;
    }
}