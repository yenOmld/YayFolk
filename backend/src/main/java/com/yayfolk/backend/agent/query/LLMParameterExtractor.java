package com.yayfolk.backend.agent.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.agent.context.ConversationContext;
import com.yayfolk.backend.agent.intent.Intent;
import com.yayfolk.backend.ai.client.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于 LLM 的智能参数提取器。
 * 替代 AgentOrchestrator 中硬编码的城市/类型关键词匹配，
 * 真正读懂用户输入中的隐含参数（非遗类型、城市、数量、价格等）。
 */
@Component
public class LLMParameterExtractor {

    private static final Logger logger = LoggerFactory.getLogger(LLMParameterExtractor.class);

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    public LLMParameterExtractor(DeepSeekClient deepSeekClient,
                                 ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 使用 LLM 从用户输入中提取结构化查询参数。
     *
     * @param userInput 用户输入
     * @param intent    已分类的意图
     * @param ctx       对话上下文（用于理解追问）
     * @return 结构化的参数 Map
     */
    public Map<String, Object> extract(String userInput, Intent intent, ConversationContext ctx) {
        try {
            String systemPrompt = buildExtractionPrompt(intent, ctx);
            String llmResponse = deepSeekClient.chat(systemPrompt, userInput);

            if (llmResponse != null) {
                return parseLLMResponse(llmResponse);
            }
        } catch (Exception e) {
            logger.warn("LLM parameter extraction failed, using keyword fallback", e);
        }

        // 降级到关键词提取
        return fallbackExtract(userInput);
    }

    /**
     * 简化版提取（不需要意图和上下文）。
     */
    public Map<String, Object> extract(String userInput) {
        return extract(userInput, Intent.RESOURCE_RECOMMEND, new ConversationContext());
    }

    private String buildExtractionPrompt(Intent intent, ConversationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个非遗活动搜索参数提取器。从用户输入中提取结构化搜索参数。\n\n");

        sb.append("请提取以下参数并以JSON格式返回（只返回JSON，不要其他内容）：\n");
        sb.append("{\n");
        sb.append("  \"heritage_type\": \"非遗类型（如：刺绣、剪纸、京剧、陶艺等），没有则为null\",\n");
        sb.append("  \"city\": \"城市名称（如：北京、苏州、杭州等），没有则为null\",\n");
        sb.append("  \"limit\": 用户要求的数量（如\"推荐3个\"则填3），默认5,\n");
        sb.append("  \"price_range\": \"价格范围：free(免费)、under_50(50以下)、under_100(100以下)、any(不限)\",\n");
        sb.append("  \"time_range\": \"时间范围：today(今天)、tomorrow(明天)、this_week(本周)、this_weekend(周末)、next_week(下周)、any(不限)\",\n");
        sb.append("  \"query_focus\": \"查询重点：activities(活动)、heritages(非遗项目)、posts(帖子)、all(全部)\",\n");
        sb.append("  \"search_keywords\": [\"搜索关键词列表，提取用户问题中的核心名词\"]\n");
        sb.append("}\n\n");

        sb.append("注意：\n");
        sb.append("1. 准确提取非遗类型。如「苏州刺绣」→ heritage_type: \"刺绣\"/\"苏绣\", city: \"苏州\"\n");
        sb.append("2. 中文数字转换为阿拉伯数字。如「三个」→ 3\n");
        sb.append("3. 识别追问场景。如果用户说「第一个在哪里」→ 这是关于上一轮结果的追问，设置 query_focus 为 \"follow_up\"\n");
        sb.append("4. 只返回JSON，不要加解释文字\n");

        // 添加对话历史
        if (ctx != null && !ctx.isEmpty()) {
            sb.append("\n").append(ctx.toPromptHistory());
        }

        return sb.toString();
    }

    private Map<String, Object> parseLLMResponse(String llmResponse) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 尝试提取 JSON 部分
            String json = llmResponse.trim();
            // 去除可能的 markdown 代码块标记
            if (json.startsWith("```")) {
                int start = json.indexOf("{");
                int end = json.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    json = json.substring(start, end + 1);
                }
            }

            Map<String, Object> parsed = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});

            // 映射到标准参数名
            if (parsed.containsKey("heritage_type") && parsed.get("heritage_type") != null) {
                result.put("heritage_type", parsed.get("heritage_type").toString().trim());
            }
            if (parsed.containsKey("city") && parsed.get("city") != null) {
                result.put("location_city", parsed.get("city").toString().trim());
                result.put("destination", parsed.get("city").toString().trim());
            }
            if (parsed.containsKey("limit") && parsed.get("limit") != null) {
                try {
                    int limit = Integer.parseInt(parsed.get("limit").toString());
                    result.put("limit", Math.min(limit, 20)); // 最多20个
                } catch (NumberFormatException ignored) {}
            }
            // 价格范围解析
            if (parsed.containsKey("price_range") && parsed.get("price_range") != null) {
                String priceRange = parsed.get("price_range").toString();
                switch (priceRange) {
                    case "free":
                        result.put("price", "免费");
                        result.put("price_max", 0);
                        break;
                    case "under_50":
                        result.put("price_max", 50);
                        break;
                    case "under_100":
                        result.put("price_max", 100);
                        break;
                    default:
                        break;
                }
            }
            // 时间范围解析
            if (parsed.containsKey("time_range") && parsed.get("time_range") != null) {
                result.put("time_range", parsed.get("time_range").toString());
            }
            // 查询焦点
            if (parsed.containsKey("query_focus") && parsed.get("query_focus") != null) {
                result.put("query_focus", parsed.get("query_focus").toString());
            }
            // 搜索关键词
            if (parsed.containsKey("search_keywords") && parsed.get("search_keywords") != null) {
                @SuppressWarnings("unchecked")
                List<String> keywords = (List<String>) parsed.get("search_keywords");
                result.put("search_keywords", keywords);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse LLM parameter extraction response: {}", llmResponse, e);
        }

        // 确保有默认值
        if (!result.containsKey("limit")) {
            result.put("limit", 5);
        }
        result.put("query", result.getOrDefault("heritage_type", "") + " "
                + result.getOrDefault("location_city", "")).toString().trim();

        return result;
    }

    /**
     * 关键词降级提取。
     */
    private Map<String, Object> fallbackExtract(String userInput) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", userInput);
        params.put("limit", 5);

        // 城市
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "苏州", "成都", "西安",
                "南京", "武汉", "重庆", "长沙", "厦门", "青岛", "大理", "昆明", "丽江",
                "洛阳", "开封", "扬州", "天津", "大连", "福州", "泉州", "拉萨", "敦煌"};
        for (String city : cities) {
            if (userInput.contains(city)) {
                params.put("location_city", city);
                params.put("destination", city);
                break;
            }
        }

        // 天数
        String normalized = userInput.replace("一", "1").replace("二", "2").replace("两", "2")
                .replace("三", "3").replace("四", "4").replace("五", "5")
                .replace("六", "6").replace("七", "7");
        for (int i = 7; i >= 1; i--) {
            if (normalized.contains(String.valueOf(i)) && (normalized.contains("天") || normalized.contains("日"))) {
                params.put("days", i);
                break;
            }
        }

        // 非遗类型
        String[] heritageTypes = {"刺绣", "剪纸", "陶艺", "皮影", "京剧", "昆曲", "书法", "国画",
                "茶艺", "木雕", "泥塑", "扎染", "花灯", "糖画", "陶瓷", "织锦",
                "印染", "漆器", "玉雕", "竹编", "年画", "面塑", "苏绣", "蜀绣", "湘绣", "粤绣"};
        for (String type : heritageTypes) {
            if (userInput.contains(type)) {
                params.put("heritage_type", type);
                break;
            }
        }

        // 数量提取 — 阿拉伯数字
        for (int i = 1; i <= 10; i++) {
            if (userInput.contains(i + "个") || userInput.contains("推荐" + i) ||
                    userInput.contains("找" + i) || userInput.contains("前" + i)) {
                params.put("limit", i);
                return params; // 找到就返回
            }
        }

        // 数量提取 — 中文数字
        String[][] chineseNums = {
                {"一", "1"}, {"二", "2"}, {"两", "2"}, {"三", "3"}, {"四", "4"},
                {"五", "5"}, {"六", "6"}, {"七", "7"}, {"八", "8"}, {"九", "9"}, {"十", "10"}
        };
        for (String[] pair : chineseNums) {
            if (userInput.contains(pair[0] + "个")) {
                params.put("limit", Integer.parseInt(pair[1]));
                break;
            }
        }

        // "几个""一些"等模糊数量 → 默认5
        if (userInput.contains("几个") || userInput.contains("一些") || userInput.contains("一点")) {
            params.put("limit", 5);
        }

        return params;
    }
}
