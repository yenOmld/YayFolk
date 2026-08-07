package com.yayfolk.backend.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.ai.config.DeepSeekConfig;
import com.yayfolk.backend.ai.model.ChatMessage;
import com.yayfolk.backend.ai.model.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 统一的 DeepSeek LLM 客户端，封装所有 HTTP 调用样板代码。
 * 所有需要调用 DeepSeek API 的 Service 都应通过此客户端进行。
 */
@Component
public class DeepSeekClient {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DeepSeekConfig config;

    public DeepSeekClient(RestTemplate restTemplate, ObjectMapper objectMapper, DeepSeekConfig config) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    /**
     * 发送非流式对话请求，返回 LLM 文本响应。
     */
    public String chat(ChatRequest request) {
        try {
            Map<String, Object> requestBody = buildRequestBody(request);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + config.getKey());

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getUrl(), HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extractContent(response.getBody());
            }
        } catch (Exception e) {
            logger.error("DeepSeek API call failed", e);
        }
        return null;
    }

    /**
     * 便捷方法：使用系统提示词和用户消息发起对话。
     */
    public String chat(String systemPrompt, String userMessage) {
        ChatRequest request = ChatRequest.simple(systemPrompt, userMessage);
        return chat(request);
    }

    /**
     * 使用系统提示词、用户消息和 Tool 定义发起对话，
     * 返回 LLM 原始响应文本（可能包含 function call JSON）。
     */
    public String chatWithTools(String systemPrompt, String userMessage, List<Map<String, Object>> tools) {
        ChatRequest request = ChatRequest.simple(systemPrompt, userMessage)
                .withTemperature(0.3);
        return chatWithToolsRaw(request, tools);
    }

    /**
     * 发起带 Tool 定义的对话请求，返回原始响应文本。
     */
    public String chatWithToolsRaw(ChatRequest request, List<Map<String, Object>> tools) {
        try {
            Map<String, Object> requestBody = buildRequestBody(request);
            if (tools != null && !tools.isEmpty()) {
                requestBody.put("tools", tools);
                requestBody.put("tool_choice", "auto");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + config.getKey());

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getUrl(), HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extractContent(response.getBody());
            }
        } catch (Exception e) {
            logger.error("DeepSeek tool call failed", e);
        }
        return null;
    }

    /**
     * 将 ChatRequest 转换为 DeepSeek API 请求体 Map。
     */
    private Map<String, Object> buildRequestBody(ChatRequest request) throws JsonProcessingException {
        // 通过序列化/反序列化将 ChatRequest 转为 Map，避免手动构造
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.convertValue(request, Map.class);
        // 将 maxTokens 映射为 DeepSeek API 的 max_tokens
        body.put("max_tokens", request.getMaxTokens());
        body.remove("maxTokens");
        body.remove("stream");  // stream 由单独方法处理
        return body;
    }

    /**
     * 从 DeepSeek API 响应中提取 assistant 消息内容。
     */
    public String extractContent(Map<String, Object> responseBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to extract content from DeepSeek response", e);
        }
        return null;
    }
}
