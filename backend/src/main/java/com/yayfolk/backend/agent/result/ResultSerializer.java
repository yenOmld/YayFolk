package com.yayfolk.backend.agent.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.agent.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ExploreResult 序列化/反序列化工具。
 * 替代 AIResourceService.serializeResources() / deserializeResources()。
 */
@Component
public class ResultSerializer {

    private static final Logger logger = LoggerFactory.getLogger(ResultSerializer.class);

    private final ObjectMapper objectMapper;

    public ResultSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 ExploreResult 序列化为 JSON 字符串（存入 ExploreMessage.resourcesJson）。
     */
    public String serialize(ExploreResult result) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            // 使用旧版 intent 名称保持前端兼容
            map.put("intent", toLegacyIntent(result.getIntent()));
            map.put("query", result.getUserQuery());
            map.put("answerText", result.getAnswerText());

            if (result.getIntent() == Intent.CHITCHAT) {
                map.put("answer", result.getAnswerText());
            } else if (result.getIntent() == Intent.ITINERARY_PLANNING) {
                map.put("itinerary", result.getItineraryText());
                map.put("activities", result.getActivityCards());
                map.put("heritages", result.getHeritageCards());
                map.putAll(result.getMetadata());
            } else if (result.getIntent() == Intent.RESOURCE_RECOMMEND) {
                map.put("activities", result.getActivityCards());
                map.put("total", result.getTotal());
            } else {
                map.put("answer", result.getAnswerText());
                map.put("posts", result.getPostCards());
                map.put("heritages", result.getHeritageCards());
                map.put("totalPosts", result.getPostCards().size());
                map.put("totalHeritages", result.getHeritageCards().size());
            }

            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize ExploreResult", e);
            return "{}";
        }
    }

    /**
     * 从 JSON 字符串反序列化为 Map（兼容旧数据格式）。
     */
    private String toLegacyIntent(Intent intent) {
        switch (intent) {
            case RESOURCE_RECOMMEND: return "STRUCTURED_QUERY";
            case ITINERARY_PLANNING: return "ITINERARY_PLANNING";
            case KNOWLEDGE_QA: return "KNOWLEDGE_QA";
            case CHITCHAT: return "CHITCHAT";
            default: return "KNOWLEDGE_QA";
        }
    }

    public Map<String, Object> deserialize(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return map;
        } catch (Exception e) {
            logger.error("Failed to deserialize resources", e);
            return new HashMap<>();
        }
    }
}
