package com.yayfolk.backend.agent.orchestration;

import com.yayfolk.backend.agent.context.ConversationContext;
import com.yayfolk.backend.agent.intent.Intent;
import com.yayfolk.backend.agent.intent.IntentClassifier;
import com.yayfolk.backend.agent.query.LLMParameterExtractor;
import com.yayfolk.backend.agent.query.TimeResolver;
import com.yayfolk.backend.agent.result.ExploreResult;
import com.yayfolk.backend.agent.tool.Tool;
import com.yayfolk.backend.agent.tool.ToolResult;
import com.yayfolk.backend.ai.client.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent 编排器：LLM 决策 → 工具调用 → 结果汇总 → 生成回复。
 *
 * 优化后的流程：
 * 1. 用户输入 + 对话历史 → 意图分类
 * 2. LLM 智能提取结构化查询参数（替代硬编码关键词）
 * 3. 根据意图 + 参数筛选可用工具
 * 4. 执行工具调用，收集结果
 * 5. LLM 汇总工具结果 + 对话历史，生成自然语言回复
 * 6. 返回 ExploreResult
 */
@Component
public class AgentOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final IntentClassifier intentClassifier;
    private final DeepSeekClient deepSeekClient;
    private final LLMParameterExtractor paramExtractor;
    private final List<Tool> tools;

    public AgentOrchestrator(IntentClassifier intentClassifier,
                              DeepSeekClient deepSeekClient,
                              LLMParameterExtractor paramExtractor,
                              List<Tool> tools) {
        this.intentClassifier = intentClassifier;
        this.deepSeekClient = deepSeekClient;
        this.paramExtractor = paramExtractor;
        this.tools = tools;
    }

    /**
     * 无历史的简单编排（向后兼容）。
     */
    public ExploreResult orchestrate(String userInput) {
        return orchestrate(userInput, new ConversationContext());
    }

    /**
     * 带对话历史的编排。
     *
     * @param userInput 用户输入
     * @param ctx       对话上下文（历史消息）
     */
    public ExploreResult orchestrate(String userInput, ConversationContext ctx) {
        // Step 1: 意图分类（传入上下文以理解追问）
        Intent intent = intentClassifier.classify(userInput, ctx);
        ExploreResult result = ExploreResult.of(intent, userInput);

        // 闲谈/打招呼：不调用任何工具，直接生成回复
        if (intent == Intent.CHITCHAT) {
            String answerText = handleChitchat(userInput, ctx);
            result.setAnswerText(answerText);
            return result;
        }

        // Step 2: LLM 智能提取结构化参数
        Map<String, Object> extractedParams = paramExtractor.extract(userInput, intent, ctx);

        // 将时间表达式解析为具体日期
        TimeResolver.resolveTimeParams(userInput, extractedParams);

        logger.info("Extracted params for '{}': {}", userInput, extractedParams);

        // Step 3: 根据意图 + 提取的参数筛选工具
        String queryFocus = extractedParams.get("query_focus") != null
                ? extractedParams.get("query_focus").toString() : null;
        List<Tool> relevantTools = filterTools(intent, queryFocus);

        // Step 4: 构建 system prompt 并执行工具调用
        String systemPrompt = buildSystemPrompt(intent);
        Map<String, ToolResult> toolResults = executeToolChain(userInput, systemPrompt,
                relevantTools, extractedParams);

        // Step 5: 填充结果卡片（先填充再截断，保证 LLM 汇总时看到的是截断后的数量）
        for (ToolResult tr : toolResults.values()) {
            if (tr.getData() == null || !tr.isSuccess()) continue;

            if (tr.getData().containsKey("activities")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> activities = (List<Map<String, Object>>) tr.getData().get("activities");
                result.getActivityCards().addAll(activities);
            }
            if (tr.getData().containsKey("posts")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> posts = (List<Map<String, Object>>) tr.getData().get("posts");
                result.getPostCards().addAll(posts);
            }
            if (tr.getData().containsKey("heritages")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> heritages = (List<Map<String, Object>>) tr.getData().get("heritages");
                result.getHeritageCards().addAll(heritages);
            }
            if (tr.getData().containsKey("itinerary")) {
                result.setItineraryText(tr.getData().get("itinerary").toString());
            }
            if (tr.getData().containsKey("destination")) {
                result.getMetadata().put("destination", tr.getData().get("destination"));
                result.getMetadata().put("days", tr.getData().get("days"));
            }
        }

        // Step 6: 按用户请求的数量截断结果卡片
        int requestedLimit = 5;
        if (extractedParams.get("limit") != null) {
            try {
                requestedLimit = Integer.parseInt(extractedParams.get("limit").toString());
            } catch (NumberFormatException ignored) {}
        }
        if (result.getActivityCards().size() > requestedLimit) {
            result.setActivityCards(new ArrayList<>(result.getActivityCards().subList(0, requestedLimit)));
        }
        if (result.getHeritageCards().size() > requestedLimit) {
            result.setHeritageCards(new ArrayList<>(result.getHeritageCards().subList(0, requestedLimit)));
        }
        if (result.getPostCards().size() > requestedLimit) {
            result.setPostCards(new ArrayList<>(result.getPostCards().subList(0, requestedLimit)));
        }
        result.setTotal(result.getActivityCards().size() + result.getPostCards().size() + result.getHeritageCards().size());

        // Step 7: LLM 汇总生成回复（现在看到的卡片已是截断后的数量）
        String answerText = synthesizeResponse(userInput, intent, toolResults, ctx, extractedParams,
                result.getActivityCards().size(), result.getHeritageCards().size());
        result.setAnswerText(answerText);

        return result;
    }

    /**
     * 执行工具调用链：根据意图和参数调用最合适的工具。
     */
    private Map<String, ToolResult> executeToolChain(String userInput, String systemPrompt,
                                                      List<Tool> relevantTools,
                                                      Map<String, Object> extractedParams) {
        Map<String, ToolResult> allResults = new LinkedHashMap<>();

        if (relevantTools.isEmpty()) {
            return allResults;
        }

        try {
            for (Tool tool : relevantTools) {
                Map<String, Object> params = buildToolParams(tool.getName(), extractedParams);
                try {
                    ToolResult tr = tool.execute(params);
                    if (tr.isSuccess()) {
                        allResults.put(tool.getName(), tr);
                    }
                } catch (Exception e) {
                    logger.warn("Tool {} execution failed: {}", tool.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Tool chain execution failed", e);
        }

        return allResults;
    }

    /**
     * 根据工具名称和 LLM 提取的参数构建工具参数。
     */
    private Map<String, Object> buildToolParams(String toolName, Map<String, Object> extractedParams) {
        Map<String, Object> params = new HashMap<>(extractedParams);

        switch (toolName) {
            case "query_activities":
                // 确保活动查询有正确的参数
                if (!params.containsKey("limit")) {
                    params.put("limit", 5);
                }
                break;
            case "search_posts_heritages":
                // 搜索工具使用 query 字段
                if (!params.containsKey("query") && params.containsKey("search_keywords")) {
                    @SuppressWarnings("unchecked")
                    List<String> keywords = (List<String>) params.get("search_keywords");
                    params.put("query", String.join(" ", keywords));
                }
                if (!params.containsKey("query")) {
                    params.put("query", params.getOrDefault("heritage_type", "").toString());
                }
                if (!params.containsKey("limit")) {
                    params.put("limit", 5);
                }
                break;
            case "plan_itinerary":
                if (!params.containsKey("destination")) {
                    params.put("destination", params.getOrDefault("location_city", "全国"));
                }
                if (!params.containsKey("days")) {
                    params.put("days", 2);
                }
                break;
            default:
                break;
        }

        return params;
    }

    /**
     * 调用 LLM 汇总工具结果，生成自然语言回复。
     */
    private String synthesizeResponse(String userInput, Intent intent,
                                       Map<String, ToolResult> toolResults,
                                       ConversationContext ctx,
                                       Map<String, Object> extractedParams,
                                       int actualActivityCount, int actualHeritageCount) {
        if (toolResults.isEmpty()) {
            return buildEmptyResponse(userInput, intent, ctx);
        }

        // 构建工具结果摘要
        StringBuilder toolOutput = new StringBuilder();
        int resultCount = 0;
        for (Map.Entry<String, ToolResult> entry : toolResults.entrySet()) {
            ToolResult tr = entry.getValue();
            toolOutput.append("【").append(entry.getKey()).append("】\n");
            toolOutput.append("摘要：").append(tr.getSummary()).append("\n");
            if (tr.getData() != null) {
                // 提取关键信息而非全部数据
                extractKeyInfo(toolOutput, tr.getData());
                // 统计结果总数
                if (tr.getData().containsKey("total")) {
                    resultCount += ((Number) tr.getData().get("total")).intValue();
                }
                if (tr.getData().containsKey("activities")) {
                    @SuppressWarnings("unchecked")
                    List<?> acts = (List<?>) tr.getData().get("activities");
                    resultCount += acts.size();
                }
            }
            toolOutput.append("\n");
        }

        try {
            String systemPrompt = buildSynthesisPrompt(intent, ctx, extractedParams,
                    actualActivityCount, actualHeritageCount);
            String prompt = "用户当前问：" + userInput + "\n\n" +
                    "实际返回：" + actualActivityCount + "个活动、" + actualHeritageCount + "个非遗项目\n\n" +
                    "工具查询结果：\n" + toolOutput.toString() + "\n" +
                    "请根据以上信息回复用户。";

            String result = deepSeekClient.chat(systemPrompt, prompt);
            if (result != null) return result;
        } catch (Exception e) {
            logger.warn("LLM synthesis failed, using default summary", e);
        }

        // 降级：拼接工具摘要
        return buildFallbackSummary(toolResults);
    }

    /**
     * 构建回复合成的 system prompt。
     */
    private String buildSynthesisPrompt(Intent intent, ConversationContext ctx,
                                         Map<String, Object> extractedParams,
                                         int actualActivityCount, int actualHeritageCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个非遗文化智能助手，帮助用户探索平台上的非遗资源。\n\n");
        sb.append("回复规则：\n");
        sb.append("1. 严格基于工具查询结果回答，不要编造任何不存在的信息\n");
        sb.append("2. 如果工具返回了具体活动/非遗项目，在回复中自然引用它们\n");
        sb.append("3. 语言亲切友好，控制在200字以内\n");
        sb.append("4. 不要使用Markdown格式\n");
        sb.append("5. 如果查询无结果，如实告知用户，并建议尝试其他关键词或放宽条件\n");
        sb.append("6. 不要推荐工具结果中没有包含的活动或项目\n");

        // 根据意图定制
        switch (intent) {
            case RESOURCE_RECOMMEND:
                sb.append("7. 用户可能在搜索活动或非遗资源，优先列出最匹配的结果\n");
                sb.append("8. 实际返回了 ").append(actualActivityCount).append(" 个活动、")
                        .append(actualHeritageCount).append(" 个非遗项目，回复中的数字必须与此一致\n");
                break;
            case KNOWLEDGE_QA:
                sb.append("7. 用户想了解非遗知识，基于搜索到的帖子和非遗项目信息作答\n");
                break;
            case ITINERARY_PLANNING:
                sb.append("7. 用户需要行程规划，基于工具返回的行程信息进行总结\n");
                break;
            default:
                break;
        }

        // 添加对话历史上下文
        if (ctx != null && !ctx.isEmpty()) {
            sb.append("\n").append(ctx.toPromptHistory());
            sb.append("注意：以上是对话历史。如果用户的当前问题是关于上一轮结果的追问，"
                    + "请结合上下文理解并回复。\n");
        }

        return sb.toString();
    }

    /**
     * 从工具数据中提取关键信息（用于 LLM 上下文，避免数据过大）。
     */
    private void extractKeyInfo(StringBuilder sb, Map<String, Object> data) {
        // 活动卡片
        if (data.containsKey("activities")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> activities = (List<Map<String, Object>>) data.get("activities");
            sb.append("活动列表(").append(activities.size()).append("个)：\n");
            int idx = 1;
            for (Map<String, Object> act : activities) {
                sb.append("  ").append(idx++).append(". ");
                sb.append(act.getOrDefault("title", "未知"));
                Object loc = act.get("location");
                if (loc != null) sb.append(" · ").append(loc);
                Object time = act.get("startTime");
                if (time != null) sb.append(" · ").append(time);
                Object price = act.get("price");
                if (price != null) {
                    sb.append(" · ");
                    try {
                        double p = Double.parseDouble(price.toString());
                        sb.append(p == 0 ? "免费" : String.format("%.2f元", p));
                    } catch (NumberFormatException e) {
                        sb.append(price).append("元");
                    }
                }
                sb.append("\n");
            }
        }

        // 非遗项目
        if (data.containsKey("heritages")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> heritages = (List<Map<String, Object>>) data.get("heritages");
            sb.append("非遗项目(").append(heritages.size()).append("个):\n");
            int idx = 1;
            for (Map<String, Object> h : heritages) {
                sb.append("  ").append(idx++).append(". ");
                sb.append(h.getOrDefault("name", "未知"));
                Object cat = h.get("category");
                if (cat != null) sb.append(" · ").append(cat);
                Object region = h.get("region");
                if (region != null) sb.append(" · ").append(region);
                sb.append("\n");
            }
        }

        // 帖子
        if (data.containsKey("posts")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> posts = (List<Map<String, Object>>) data.get("posts");
            sb.append("相关帖子(").append(posts.size()).append("篇)\n");
        }
    }

    /**
     * 无结果时的智能回复。
     */
    private String buildEmptyResponse(String userInput, Intent intent, ConversationContext ctx) {
        // 如果是追问且上一轮有结果，提示用户查看上一轮结果
        if (ctx != null && ctx.isFollowUpQuestion(userInput) && !ctx.isEmpty()) {
            return "关于您的问题，请参考上面我为您找到的内容。如果需要更具体的信息，可以告诉我您想了解什么。";
        }

        switch (intent) {
            case RESOURCE_RECOMMEND:
                return "抱歉，暂时没有找到完全匹配的活动或资源。\n\n建议您：\n· 尝试更宽泛的关键词搜索\n· 更换非遗类型或城市\n· 查看平台首页的热门推荐";
            case ITINERARY_PLANNING:
                return "暂时无法为该目的地规划详细行程。建议您：\n· 尝试其他非遗热门城市（如北京、苏州、杭州）\n· 减少旅行天数\n· 使用更具体的目的地名称";
            case KNOWLEDGE_QA:
                return "关于这个问题，目前平台暂时没有收录详细的资料。我会继续学习更多非遗知识，您也可以尝试在社区发帖提问。";
            default:
                return "抱歉，我暂时无法处理您的请求。请尝试换个方式描述您的需求。";
        }
    }

    private String buildFallbackSummary(Map<String, ToolResult> toolResults) {
        StringBuilder fallback = new StringBuilder();
        for (ToolResult tr : toolResults.values()) {
            if (fallback.length() > 0) fallback.append("；");
            fallback.append(tr.getSummary());
        }
        return fallback.toString();
    }

    /**
     * 处理闲谈/打招呼：不调用工具，直接用 LLM 生成友好回复。
     */
    private String handleChitchat(String userInput, ConversationContext ctx) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("你是「YayFolk非遗」平台的智能助手，专门帮助用户探索中国非物质文化遗产。\n");
            sb.append("你可以帮助用户：\n");
            sb.append("- 搜索和推荐非遗活动\n");
            sb.append("- 介绍非遗项目和技艺知识\n");
            sb.append("- 规划非遗主题旅行路线\n\n");
            sb.append("请用友好亲切的语气回复用户的问候或问题。控制在100字以内。不要使用Markdown。\n");

            if (ctx != null && !ctx.isEmpty()) {
                sb.append("\n").append(ctx.toPromptHistory());
            }

            String result = deepSeekClient.chat(sb.toString(), userInput);
            if (result != null) return result;
        } catch (Exception e) {
            logger.warn("Chitchat LLM call failed", e);
        }

        // 降级：预置回复
        if (userInput.contains("你是谁") || userInput.contains("你叫什么") || userInput.contains("你的名字")) {
            return "你好！我是YayFolk非遗智能助手，可以帮你搜索非遗活动、了解非遗知识、规划非遗旅行路线。有什么想探索的吗？";
        }
        if (userInput.contains("你好") || userInput.contains("嗨") || userInput.contains("hi")) {
            return "你好！有什么非遗相关的问题想问我吗？";
        }
        if (userInput.contains("谢谢") || userInput.contains("感谢")) {
            return "不客气！有需要随时找我。";
        }
        if (userInput.contains("再见") || userInput.contains("拜拜")) {
            return "再见！期待下次为你探索非遗文化～";
        }
        if (userInput.contains("你能做什么") || userInput.contains("你会什么")) {
            return "我可以帮你：\n· 搜索推荐非遗活动\n· 介绍非遗知识和技艺\n· 规划非遗旅行路线\n有什么想了解的？";
        }
        return "你好！我是YayFolk非遗智能助手，有什么可以帮你的吗？";
    }

    private String buildSystemPrompt(Intent intent) {
        String base = "你是一个非遗文化智能助手，帮助用户探索平台上的非遗资源。\n";

        switch (intent) {
            case RESOURCE_RECOMMEND:
                return base + "用户想要查找非遗相关资源（活动、帖子、项目）。\n" +
                        "你可以使用 query_activities 查询活动，使用 search_posts_heritages 搜索帖子和非遗项目。\n" +
                        "根据用户的具体需求选择最合适的工具。";
            case ITINERARY_PLANNING:
                return base + "用户想要规划非遗旅行路线。\n" +
                        "你可以使用 plan_itinerary 生成行程规划。";
            case KNOWLEDGE_QA:
                return base + "用户询问非遗相关知识。\n" +
                        "你可以使用 search_posts_heritages 搜索相关帖子和非遗项目作为参考资料。";
            default:
                return base;
        }
    }

    /**
     * 根据意图和查询焦点筛选工具。
     * query_focus 仅在 RESOURCE_RECOMMEND 意图下细化工具选择，
     * 其他意图（行程规划、知识问答）由意图本身决定工具，不受 query_focus 影响。
     */
    private List<Tool> filterTools(Intent intent, String queryFocus) {
        List<Tool> filtered = new ArrayList<>();
        Map<String, Tool> toolMap = new HashMap<>();
        for (Tool t : tools) {
            toolMap.put(t.getName(), t);
        }

        // 追问场景（任何意图）：不调用工具，由 synthesizeResponse 处理
        if ("follow_up".equals(queryFocus)) {
            return filtered;
        }

        // CHITCHAT 不调用任何工具
        if (intent == Intent.CHITCHAT) {
            return filtered;
        }

        // query_focus 仅在 RESOURCE_RECOMMEND 意图下细化工具选择
        if (intent == Intent.RESOURCE_RECOMMEND && queryFocus != null) {
            switch (queryFocus) {
                case "activities":
                    addIfExists(toolMap, filtered, "query_activities");
                    return filtered;
                case "heritages":
                case "posts":
                    addIfExists(toolMap, filtered, "search_posts_heritages");
                    return filtered;
                case "all":
                    addIfExists(toolMap, filtered, "query_activities");
                    addIfExists(toolMap, filtered, "search_posts_heritages");
                    return filtered;
                default:
                    break;
            }
        }

        // 默认按意图筛选
        switch (intent) {
            case RESOURCE_RECOMMEND:
                // 默认同时查询活动和帖子/非遗，确保覆盖全面
                addIfExists(toolMap, filtered, "query_activities");
                addIfExists(toolMap, filtered, "search_posts_heritages");
                break;
            case ITINERARY_PLANNING:
                addIfExists(toolMap, filtered, "plan_itinerary");
                break;
            case KNOWLEDGE_QA:
                addIfExists(toolMap, filtered, "search_posts_heritages");
                break;
            default:
                filtered.addAll(tools);
        }
        return filtered;
    }

    private void addIfExists(Map<String, Tool> toolMap, List<Tool> list, String name) {
        if (toolMap.containsKey(name)) {
            list.add(toolMap.get(name));
        }
    }
}
