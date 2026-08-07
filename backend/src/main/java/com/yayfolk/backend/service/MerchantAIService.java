package com.yayfolk.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.ai.client.DeepSeekClient;
import com.yayfolk.backend.ai.model.ChatRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MerchantAIService {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    public MerchantAIService(DeepSeekClient deepSeekClient, ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> generateMerchantSuggestions(Map<String, Object> merchantData) {
        try {
            String systemPrompt = "你是一名专业的商业顾问，擅长分析商家运营数据并提供优化建议。\n" +
                    "请基于用户提供的真实数据，返回JSON格式的分析和建议。\n" +
                    "JSON必须包含analysis和suggestions字段，不要返回其他内容。\n" +
                    "请使用中文返回所有内容。";

            Map<String, Object> activitiesData = (Map<String, Object>) merchantData.getOrDefault("activities", new ArrayList<>());
            String activitiesStr = activitiesData.isEmpty() ? "无活动数据" :
                activitiesData.values().stream()
                    .limit(5)
                    .map(a -> {
                        Map<String, Object> act = (Map<String, Object>) a;
                        return (act.get("title") != null ? act.get("title") : "未命名活动")
                            + " (" + act.getOrDefault("participantCount", 0) + " 参与人数, 评分 " + act.getOrDefault("averageRating", 0) + ", 收入 ¥" + act.getOrDefault("revenue", 0) + ")";
                    })
                    .reduce("", (s1, s2) -> s1.isEmpty() ? s2 : s1 + "\n" + s2);

            List<Map<String, Object>> reviewsData = (List<Map<String, Object>>) merchantData.getOrDefault("reviews", new ArrayList<>());
            String reviewsStr = reviewsData.isEmpty() ? "无评价数据" :
                reviewsData.stream()
                    .map(r -> "- " + r.getOrDefault("score", 0) + " 星：" + r.getOrDefault("content", ""))
                    .reduce("", (s1, s2) -> s1.isEmpty() ? s2 : s1 + "\n" + s2);

            String userPrompt = String.format(
                "请分析以下商家的真实数据并返回JSON格式的建议：\n\n商家数据：\n- 活动总数：%d\n- 订单总数：%d\n- 评价总数：%d\n- 平均评分：%s\n- 总收入：¥%s\n\n热门活动：\n%s\n\n最近评价：\n%s\n\n请返回以下JSON格式（不要包含markdown代码块，直接返回纯JSON）：\n{\n  \"analysis\": {\n    \"operationStatus\": \"运营状态评估\",\n    \"customerSatisfaction\": \"客户满意度评估\",\n    \"revenueTrend\": \"收入趋势评估\"\n  },\n  \"suggestions\": [\n    {\n      \"title\": \"建议标题\",\n      \"description\": \"详细描述\",\n      \"tips\": [\"步骤1\", \"步骤2\"],\n      \"priority\": \"high或medium或low\",\n      \"icon\": \"bx图标类名\",\n      \"color\": {\"bg\": \"#背景颜色\", \"text\": \"#文本颜色\"}\n    }\n  ]\n}",
                (int) merchantData.getOrDefault("activityCount", 0),
                (int) merchantData.getOrDefault("orderCount", 0),
                (int) merchantData.getOrDefault("reviewCount", 0),
                merchantData.getOrDefault("averageRating", "0"),
                merchantData.getOrDefault("totalRevenue", "0"),
                activitiesStr,
                reviewsStr
            );

            ChatRequest request = ChatRequest.simple(systemPrompt, userPrompt)
                    .withTemperature(0.5)
                    .withMaxTokens(2500);

            String aiContent = deepSeekClient.chat(request);

            if (aiContent != null) {
                String jsonStr = aiContent.trim();
                int startIdx = jsonStr.indexOf("{");
                int endIdx = jsonStr.lastIndexOf("}");
                if (startIdx >= 0 && endIdx > startIdx) {
                    jsonStr = jsonStr.substring(startIdx, endIdx + 1);
                }

                return objectMapper.readValue(jsonStr, Map.class);
            }

            return generateDefaultResult(merchantData);

        } catch (Exception e) {
            e.printStackTrace();
            return generateDefaultResult(merchantData);
        }
    }

    private Map<String, Object> generateDefaultResult(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();

        Map<String, String> analysis = new HashMap<>();
        int activityCount = (int) data.getOrDefault("activityCount", 0);
        int orderCount = (int) data.getOrDefault("orderCount", 0);
        double avgRating = 0;
        try {
            avgRating = Double.parseDouble(data.getOrDefault("averageRating", "0").toString());
        } catch (Exception e) {}

        String operationStatus = "一般";
        if (activityCount > 10 && orderCount > 100) operationStatus = "优秀";
        else if (activityCount > 5 && orderCount > 50) operationStatus = "良好";

        String customerSatisfaction = "一般";
        if (avgRating >= 4.5) customerSatisfaction = "非常高";
        else if (avgRating >= 4.0) customerSatisfaction = "高";
        else if (avgRating >= 3.0) customerSatisfaction = "一般";
        else if (avgRating > 0) customerSatisfaction = "需要改进";

        double revenue = 0;
        try {
            revenue = Double.parseDouble(data.getOrDefault("totalRevenue", "0").toString());
        } catch (Exception e) {}

        String revenueTrend = "一般";
        if (revenue > 10000) revenueTrend = "良好上升趋势";
        else if (revenue > 5000) revenueTrend = "稳定增长";
        else if (revenue > 0) revenueTrend = "有提升空间";
        else revenueTrend = "无收入数据";

        analysis.put("operationStatus", operationStatus);
        analysis.put("customerSatisfaction", customerSatisfaction);
        analysis.put("revenueTrend", revenueTrend);
        result.put("analysis", analysis);

        List<Map<String, Object>> suggestions = new ArrayList<>();

        if (activityCount == 0) {
            Map<String, Object> s = new HashMap<>();
            s.put("title", "创建更多活动");
            s.put("description", "您还未创建任何活动。建议根据您的业务特点创建有吸引力的活动，以吸引用户参与。");
            s.put("tips", Arrays.asList("分析目标用户群体的兴趣和需求", "根据节假日、季节等时间节点策划活动", "设计互动性强的活动内容"));
            s.put("priority", "high");
            s.put("icon", "bx bxs-calendar-plus");
            s.put("color", new HashMap<String, String>() {{ put("bg", "#e6f7ff"); put("text", "#1890ff"); }});
            suggestions.add(s);
        }

        if (suggestions.isEmpty()) {
            Map<String, Object> s = new HashMap<>();
            s.put("title", "持续优化运营");
            s.put("description", "您的数据表现良好，建议保持并持续优化。");
            s.put("tips", Arrays.asList("定期分析活动数据", "关注用户反馈趋势", "尝试新的活动形式", "建立用户社区增强粘性"));
            s.put("priority", "low");
            s.put("icon", "bx bxs-rocket");
            s.put("color", new HashMap<String, String>() {{ put("bg", "#e6f7ff"); put("text", "#1890ff"); }});
            suggestions.add(s);
        }

        result.put("suggestions", suggestions);
        return result;
    }
}
