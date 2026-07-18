package com.yayfolk.backend.agent.tool;

import com.yayfolk.backend.agent.query.ActivityQueryBuilder;
import com.yayfolk.backend.agent.query.QueryParams;
import com.yayfolk.backend.entity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 活动查询工具：按结构化条件查询活动列表。
 * 仅返回匹配的活动；无匹配时返回空结果（不再降级到热门活动，避免无关推荐）。
 */
@Component
public class QueryActivitiesTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(QueryActivitiesTool.class);

    private final ActivityQueryBuilder activityQueryBuilder;

    public QueryActivitiesTool(ActivityQueryBuilder activityQueryBuilder) {
        this.activityQueryBuilder = activityQueryBuilder;
    }

    @Override
    public String getName() {
        return "query_activities";
    }

    @Override
    public String getDescription() {
        return "按条件查询非遗活动。可根据城市、非遗类型、价格、日期等筛选。适合用户想找活动、查看活动信息等场景。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        String[][] paramDefs = {
                {"heritage_type", "string", "非遗类型，如：刺绣、剪纸、陶艺、皮影、京剧等"},
                {"location_city", "string", "城市名称，如：北京、上海、杭州等"},
                {"price_max", "number", "价格上限（元），如50表示50元以下"},
                {"start_date", "string", "精确日期，格式 yyyy-MM-dd"},
                {"start_date_after", "string", "开始日期下限，格式 yyyy-MM-dd"},
                {"start_date_before", "string", "开始日期上限，格式 yyyy-MM-dd"},
        };

        for (String[] def : paramDefs) {
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("type", def[1]);
            prop.put("description", def[2]);
            properties.put(def[0], prop);
        }

        schema.put("properties", properties);
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        QueryParams params = QueryParams.fromLLMResponse(parameters);

        // 从参数中提取 limit（LLM 提取的数量）
        int requestedLimit = 5;
        if (parameters.containsKey("limit")) {
            try {
                requestedLimit = Integer.parseInt(parameters.get("limit").toString());
            } catch (NumberFormatException ignored) {}
        }
        // 多查几个做缓冲，但最终按请求数量截断
        List<Activity> activities = activityQueryBuilder.query(params, Math.min(Math.max(requestedLimit, 5) + 5, 50));

        // 不再返回无关的热门活动作为降级方案
        // 空结果意味着没有完全匹配的内容，应如实告知用户

        // 按请求数量截断
        if (activities.size() > requestedLimit) {
            activities = activities.subList(0, requestedLimit);
        }

        List<Map<String, Object>> cards = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (Activity activity : activities) {
            Map<String, Object> card = new HashMap<>();
            card.put("id", activity.getId());
            card.put("title", activity.getTitle());
            card.put("subtitle", activity.getSubtitle());
            card.put("heritageType", activity.getHeritageType());
            card.put("location", (activity.getLocationCity() != null ? activity.getLocationCity() : "") +
                    (activity.getLocationDetail() != null ? " " + activity.getLocationDetail() : ""));
            card.put("startTime", activity.getStartTime() != null ? sdf.format(activity.getStartTime()) : "");
            card.put("price", activity.getPrice() != null ? activity.getPrice() / 100.0 : 0);
            card.put("status", activity.getStatus());
            cards.add(card);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("activities", cards);
        data.put("total", cards.size());

        String summary = cards.isEmpty()
                ? "未找到符合条件的活动，建议放宽筛选条件"
                : "为您找到 " + cards.size() + " 个相关活动";
        return ToolResult.success(summary, data);
    }
}
