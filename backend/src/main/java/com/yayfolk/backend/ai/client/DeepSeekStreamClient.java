package com.yayfolk.backend.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.ai.config.DeepSeekConfig;
import com.yayfolk.backend.ai.model.ChatMessage;
import com.yayfolk.backend.ai.model.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek SSE 流式响应客户端。
 * 用于知识问答等需要逐字输出流式体验的场景。
 */
@Component
public class DeepSeekStreamClient {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekStreamClient.class);

    private final ObjectMapper objectMapper;
    private final DeepSeekConfig config;

    public DeepSeekStreamClient(ObjectMapper objectMapper, DeepSeekConfig config) {
        this.objectMapper = objectMapper;
        this.config = config;
    }

    /**
     * 发起流式对话请求，返回逐字的 Flux<String>。
     */
    public Flux<String> streamChat(ChatRequest request) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModel());
        requestBody.put("messages", buildMessagesList(request));
        requestBody.put("max_tokens", request.getMaxTokens());
        requestBody.put("temperature", request.getTemperature());
        requestBody.put("stream", true);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + config.getKey());
        headers.set("Accept", "text/event-stream");

        WebClient webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        return webClient.post()
                .uri(config.getUrl())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(requestBody)
                .exchangeToFlux(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToFlux(String.class)
                                .filter(chunk -> chunk != null && !chunk.trim().isEmpty())
                                .flatMap(chunk -> {
                                    String trimmedChunk = chunk.trim();

                                    // SSE 格式以 "data:" 开头
                                    if (trimmedChunk.startsWith("data:")) {
                                        trimmedChunk = trimmedChunk.substring(5).trim();
                                    }

                                    // 结束标记
                                    if ("[DONE]".equals(trimmedChunk)) {
                                        return Flux.empty();
                                    }

                                    try {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> data = objectMapper.readValue(trimmedChunk, Map.class);
                                        String content = extractDeltaContent(data);
                                        if (content != null && !content.isEmpty()) {
                                            return Flux.just(content);
                                        }
                                    } catch (Exception e) {
                                        // 忽略解析失败的分块
                                    }

                                    return Flux.empty();
                                });
                    } else {
                        logger.error("DeepSeek stream API returned status: {}", response.statusCode());
                        return Flux.just("抱歉，AI暂时无法回复，请稍后再试。");
                    }
                });
    }

    /**
     * 便捷方法：使用系统提示词和用户消息发起流式对话。
     */
    public Flux<String> streamChat(String systemPrompt, String userMessage) {
        ChatRequest request = ChatRequest.simple(systemPrompt, userMessage);
        return streamChat(request);
    }

    /**
     * 便捷方法：使用消息列表发起流式对话。
     */
    public Flux<String> streamChat(List<Map<String, Object>> messages) {
        ChatRequest request = new ChatRequest();
        request.setMessages(convertMessages(messages));
        request.setMaxTokens(200);
        request.setTemperature(0.7);
        return streamChat(request);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildMessagesList(ChatRequest request) {
        return objectMapper.convertValue(request.getMessages(), List.class);
    }

    private List<ChatMessage> convertMessages(List<Map<String, Object>> messages) {
        List<ChatMessage> result = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            String role = (String) msg.get("role");
            String content = (String) msg.get("content");
            result.add(new ChatMessage(role, content));
        }
        return result;
    }

    /**
     * 从 SSE 数据块中提取 delta.content。
     */
    private String extractDeltaContent(Map<String, Object> chunk) {
        if (chunk.containsKey("choices")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                if (choice.containsKey("delta")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                    if (delta.containsKey("content")) {
                        return (String) delta.get("content");
                    }
                }
            }
        }
        return null;
    }
}
