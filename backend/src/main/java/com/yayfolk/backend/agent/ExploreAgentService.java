package com.yayfolk.backend.agent;

import com.yayfolk.backend.agent.context.ConversationContext;
import com.yayfolk.backend.agent.intent.Intent;
import com.yayfolk.backend.agent.orchestration.AgentOrchestrator;
import com.yayfolk.backend.agent.result.ExploreResult;
import com.yayfolk.backend.agent.result.ResultSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 探索资源 Agent Service。
 * 替代 AIResourceService，提供探索平台资源的统一入口。
 *
 * 支持多轮对话上下文记忆：通过 ConversationContext 传入历史消息，
 * Agent 可以理解追问、引用先前结果。
 */
@Service
public class ExploreAgentService {

    private static final Logger logger = LoggerFactory.getLogger(ExploreAgentService.class);

    private final AgentOrchestrator agentOrchestrator;
    private final ResultSerializer resultSerializer;

    public ExploreAgentService(AgentOrchestrator agentOrchestrator,
                                ResultSerializer resultSerializer) {
        this.agentOrchestrator = agentOrchestrator;
        this.resultSerializer = resultSerializer;
    }

    /**
     * 执行探索资源查询（无历史上下文，向后兼容）。
     *
     * @param userInput 用户自然语言输入
     * @return ExploreResult 包含意图、自然语言回复、资源卡片
     */
    public ExploreResult explore(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            throw new RuntimeException("请输入查询内容");
        }

        logger.info("Explore agent processing: {}", userInput);
        return agentOrchestrator.orchestrate(userInput.trim());
    }

    /**
     * 执行探索资源查询（带对话历史，支持上下文记忆）。
     *
     * @param userInput 用户自然语言输入
     * @param history   对话历史消息列表，每条包含 "role" 和 "content"
     * @return ExploreResult
     */
    public ExploreResult explore(String userInput, List<Map<String, String>> history) {
        if (userInput == null || userInput.trim().isEmpty()) {
            throw new RuntimeException("请输入查询内容");
        }

        ConversationContext ctx = ConversationContext.fromHistory(history);
        logger.info("Explore agent processing with {} history messages: {}", ctx.size(), userInput);
        return agentOrchestrator.orchestrate(userInput.trim(), ctx);
    }

    /**
     * 兼容旧接口：接受 Map<String, Object> request 参数。
     * 供过渡期 AIController 使用。
     */
    public Map<String, Object> exploreAsMap(Map<String, Object> request) {
        String userInput = request.get("userInput") == null ? "" : request.get("userInput").toString().trim();
        if (userInput.isEmpty()) {
            throw new RuntimeException("请输入查询内容");
        }

        ExploreResult result = explore(userInput);

        // 转换为旧格式
        String intentStr = result.getIntent().name();
        if (result.getIntent() == Intent.RESOURCE_RECOMMEND) {
            intentStr = "STRUCTURED_QUERY";
        } else if (result.getIntent() == Intent.KNOWLEDGE_QA) {
            intentStr = "KNOWLEDGE_QA";
        } else if (result.getIntent() == Intent.ITINERARY_PLANNING) {
            intentStr = "ITINERARY_PLANNING";
        } else if (result.getIntent() == Intent.CHITCHAT) {
            intentStr = "CHITCHAT";
        }

        String summary = result.buildSummary();
        String resourcesJson = resultSerializer.serialize(result);

        Map<String, Object> mapResult = new java.util.LinkedHashMap<>();
        mapResult.put("intent", intentStr);
        mapResult.put("query", result.getUserQuery());
        mapResult.put("_summary", summary);
        mapResult.put("_resourcesJson", resourcesJson);
        mapResult.put("_result", result);
        return mapResult;
    }

    /**
     * 生成摘要文本。
     */
    public String buildSummaryText(ExploreResult result) {
        return result.buildSummary();
    }

    /**
     * 序列化资源为 JSON。
     */
    public String serializeResources(ExploreResult result) {
        return resultSerializer.serialize(result);
    }

    /**
     * 反序列化 JSON 为资源 Map。
     */
    public Map<String, Object> deserializeResources(String json) {
        return resultSerializer.deserialize(json);
    }
}
