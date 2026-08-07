package com.yayfolk.backend.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek/Chat API 请求体。
 */
public class ChatRequest {

    private String model = "deepseek-chat";
    private List<ChatMessage> messages = new ArrayList<>();
    private double temperature = 0.7;
    private int maxTokens = 800;
    private boolean stream = false;

    public static ChatRequest simple(String systemPrompt, String userMessage) {
        ChatRequest request = new ChatRequest();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            request.messages.add(ChatMessage.system(systemPrompt));
        }
        request.messages.add(ChatMessage.user(userMessage));
        return request;
    }

    public ChatRequest addMessage(ChatMessage message) {
        this.messages.add(message);
        return this;
    }

    public ChatRequest withTemperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    public ChatRequest withMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public ChatRequest withStream(boolean stream) {
        this.stream = stream;
        return this;
    }

    // Getters and setters

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
}
