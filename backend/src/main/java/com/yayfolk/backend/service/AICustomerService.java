package com.yayfolk.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.entity.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.net.InetSocketAddress;
import java.net.Proxy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// AI辅助生成：DeepSeek API（deepseek-v4-flash），2025-04-26
@Service
public class AICustomerService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${HTTP_PROXY_HOST:}")
    private String httpProxyHost;

    @Value("${HTTP_PROXY_PORT:}")
    private String httpProxyPort;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public AICustomerService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        
        // 配置RestTemplate
        if (httpProxyHost != null && !httpProxyHost.isEmpty() && httpProxyPort != null && !httpProxyPort.isEmpty()) {
            // 创建带有代理的RestTemplate
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost, Integer.parseInt(httpProxyPort)));
            factory.setProxy(proxy);
            this.restTemplate = new RestTemplate(factory);
        } else {
            this.restTemplate = restTemplate;
        }
        
        // 配置WebClient
        WebClient.Builder webClientBuilder = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024));
        
        // 如果配置了代理，添加代理设置
        if (httpProxyHost != null && !httpProxyHost.isEmpty() && httpProxyPort != null && !httpProxyPort.isEmpty()) {
            System.setProperty("http.proxyHost", httpProxyHost);
            System.setProperty("http.proxyPort", httpProxyPort);
            System.setProperty("https.proxyHost", httpProxyHost);
            System.setProperty("https.proxyPort", httpProxyPort);
        }
        
        this.webClient = webClientBuilder.build();
    }

    // 非流式响应（保持不变）
    public String generateResponse(List<Message> conversationHistory, String latestMessage) {
        List<Map<String, Object>> messages = buildMessages(conversationHistory, latestMessage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 200);
        requestBody.put("temperature", 0.7);
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + apiKey);

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        if (choice.containsKey("message")) {
                            Map<String, Object> message = (Map<String, Object>) choice.get("message");
                            if (message.containsKey("content")) {
                                return (String) message.get("content");
                            }
                        }
                    }
                }
                return "抱歉，AI客服暂时无法回复，请稍后再试。";
            } else {
                return "抱歉，AI客服暂时无法回复，请稍后再试。";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，AI客服暂时无法回复，请稍后再试。";
        }
    }

    // 流式响应（修正版）
    public Flux<String> streamGenerateResponse(List<Message> conversationHistory, String latestMessage) {
        List<Map<String, Object>> messages = buildMessages(conversationHistory, latestMessage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 200);
        requestBody.put("temperature", 0.7);
        requestBody.put("stream", true);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "text/event-stream");

        return webClient.post()
                .uri(apiUrl)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(requestBody)
                .exchangeToFlux(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        // DeepSeek API 返回标准 SSE 格式，每个数据块以 "data:" 开头
                        return response.bodyToFlux(String.class)
                                .filter(chunk -> chunk != null && !chunk.trim().isEmpty())
                                .flatMap(chunk -> {
                                    System.out.println("收到原始数据块: " + chunk);
                                    
                                    String trimmedChunk = chunk.trim();
                                    
                                    // SSE 格式以 "data:" 开头，需要去除
                                    if (trimmedChunk.startsWith("data:")) {
                                        trimmedChunk = trimmedChunk.substring(5).trim();
                                    }
                                    
                                    // 检查是否为结束标记
                                    if (trimmedChunk.equals("[DONE]")) {
                                        System.out.println("收到结束标记");
                                        return Flux.empty();
                                    }
                                    
                                    try {
                                        // 解析去除 data: 前缀后的 JSON 数据块
                                        Map<String, Object> data = objectMapper.readValue(trimmedChunk, Map.class);
                                        System.out.println("解析 JSON 数据: " + data);
                                        String content = extractContentFromChunk(data);
                                        if (content != null && !content.isEmpty()) {
                                            System.out.println("提取到内容: " + content);
                                            return Flux.just(content);
                                        }
                                    } catch (Exception e) {
                                        // 解析失败，忽略该片段
                                        System.err.println("JSON 解析失败: " + e.getMessage());
                                        System.err.println("原始数据: " + chunk);
                                    }
                                    
                                    return Flux.empty();
                                });
                    } else {
                        System.err.println("DeepSeek API 响应状态: " + response.statusCode());
                        return Flux.just("抱歉，AI客服暂时无法回复，请稍后再试。");
                    }
                });
    }

    // 从单个 SSE 数据块中提取 content
    private String extractContentFromChunk(Map<String, Object> chunk) {
        if (chunk.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                if (choice.containsKey("delta")) {
                    Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                    if (delta.containsKey("content")) {
                        String content = (String) delta.get("content");
                        return content;
                    }
                }
            }
        }
        return null;
    }



    // 构建消息列表（系统提示 + 历史 + 当前消息）
    private List<Map<String, Object>> buildMessages(List<Message> conversationHistory, String latestMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 系统提示
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个智能客服助手，名为Yaya。你的任务是友好、专业地回答用户有关中国传统非物质文化遗产（非遗）的问题，提供帮助和支持。\n\n重要要求：\n1. 请使用自然流畅的中文回复，避免使用Markdown格式（如**粗体**、--分割线、#标题等）\n2. 请确保回复内容简洁明了，直接回答问题，不超过300字。\n3. 只提供必要的信息，避免冗长的解释和多余的内容。\n4. 你只能回答与中国传统非遗相关的问题，包括非遗的历史、特点、传承、保护等方面。\n5. 对于偏离中国非遗主题的问题，请礼貌地提示用户：'和Yaya一起来探索博大精深的中国传统非物质文化遗产吧！'\n6. 确保你的回答始终与中国非遗相关，不要涉及其他无关内容。");
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