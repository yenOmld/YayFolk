package com.yayfolk.backend.agent.tool;

import com.yayfolk.backend.ai.client.DeepSeekClient;
import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.IntangibleCulturalHeritage;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.IntangibleCulturalHeritageRepository;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 行程规划工具：根据目的地和天数生成非遗旅行路线。
 */
@Component
public class PlanItineraryTool implements Tool {

    private final DeepSeekClient deepSeekClient;
    private final ActivityRepository activityRepository;
    private final IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository;

    public PlanItineraryTool(DeepSeekClient deepSeekClient,
                              ActivityRepository activityRepository,
                              IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository) {
        this.deepSeekClient = deepSeekClient;
        this.activityRepository = activityRepository;
        this.intangibleCulturalHeritageRepository = intangibleCulturalHeritageRepository;
    }

    @Override
    public String getName() {
        return "plan_itinerary";
    }

    @Override
    public String getDescription() {
        return "为用户规划非遗旅行路线。需要目的地和天数。返回每天的行程安排、相关活动和景点。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> destProp = new LinkedHashMap<>();
        destProp.put("type", "string");
        destProp.put("description", "目的地城市，如：北京、苏州、杭州");
        properties.put("destination", destProp);

        Map<String, Object> daysProp = new LinkedHashMap<>();
        daysProp.put("type", "integer");
        daysProp.put("description", "旅行天数，默认2天");
        properties.put("days", daysProp);

        schema.put("properties", properties);
        schema.put("required", Collections.singletonList("destination"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String destination = parameters.get("destination") != null
                ? parameters.get("destination").toString() : "全国";
        int days = 2;
        if (parameters.get("days") != null) {
            try {
                days = Integer.parseInt(parameters.get("days").toString());
            } catch (NumberFormatException ignored) {}
        }

        // 查找目的地相关活动和遗产
        List<Activity> activities = findActivitiesByDestination(destination);
        List<IntangibleCulturalHeritage> heritages = findHeritagesByDestination(destination);

        List<Activity> selectedActivities = activities.stream().limit(days * 2L).collect(Collectors.toList());
        List<IntangibleCulturalHeritage> selectedHeritages = heritages.stream().limit(days * 2L).collect(Collectors.toList());

        // 用 LLM 生成行程
        String itineraryText = generateItinerary(destination, days, selectedActivities, selectedHeritages);

        Map<String, Object> data = new HashMap<>();
        data.put("destination", destination);
        data.put("days", days);
        data.put("itinerary", itineraryText);
        data.put("activities", buildActivityCards(selectedActivities));
        data.put("heritages", buildHeritageCards(selectedHeritages));

        return ToolResult.success("为您规划了" + days + "天" + destination + "非遗之旅", data);
    }

    private String generateItinerary(String destination, int days,
                                      List<Activity> activities, List<IntangibleCulturalHeritage> heritages) {
        StringBuilder context = new StringBuilder();
        context.append("目的地：").append(destination).append("\n");
        context.append("天数：").append(days).append("天\n\n");

        if (!activities.isEmpty()) {
            context.append("【可选活动】\n");
            for (Activity a : activities) {
                context.append("- ID:").append(a.getId()).append(" 标题：").append(a.getTitle());
                if (a.getHeritageType() != null) context.append(" 非遗类型：").append(a.getHeritageType());
                if (a.getLocationCity() != null) context.append(" 地点：").append(a.getLocationCity());
                if (a.getPrice() != null) context.append(" 价格：").append(a.getPrice() == 0 ? "免费" : String.format("%.2f元", a.getPrice() / 100.0));
                context.append("\n");
            }
        }

        if (!heritages.isEmpty()) {
            context.append("【可选非遗项目】\n");
            for (IntangibleCulturalHeritage h : heritages) {
                context.append("- ID:").append(h.getId()).append(" 名称：").append(h.getName());
                if (h.getCategory() != null) context.append(" 类别：").append(h.getCategory());
                if (h.getRegion() != null) context.append(" 地区：").append(h.getRegion());
                if (h.getIntroduction() != null) {
                    String intro = h.getIntroduction();
                    if (intro.length() > 100) intro = intro.substring(0, 100) + "...";
                    context.append(" 简介：").append(intro);
                }
                context.append("\n");
            }
        }

        try {
            String systemPrompt = "你是一个专业的非遗旅行规划师。根据用户需求和提供的活动与非遗项目资源，生成详细的行程规划。\n" +
                    "规则：\n1. 按天规划，每天安排2-3个活动或项目\n2. 考虑地理位置，尽量同一区域的活动在同一天\n" +
                    "3. 每天给出上午、下午、晚上的安排\n" +
                    "4. 提到活动和项目时，直接使用标题或名称，不要加任何ID或标记前缀\n" +
                    "5. 语言简洁友好，不超过500字\n6. 不要使用Markdown格式";

            String result = deepSeekClient.chat(
                    systemPrompt,
                    "可用资源：\n" + context + "\n\n请为我在" + destination + "规划" + days + "天的非遗之旅");
            if (result != null) return result;
        } catch (Exception e) {
            // 降级到规则生成
        }

        return buildFallbackItinerary(destination, days, activities, heritages);
    }

    private String buildFallbackItinerary(String destination, int days,
                                           List<Activity> activities, List<IntangibleCulturalHeritage> heritages) {
        StringBuilder sb = new StringBuilder();
        sb.append("为您规划").append(days).append("天").append(destination).append("非遗之旅：\n\n");

        int actIdx = 0, herIdx = 0;
        for (int d = 1; d <= days; d++) {
            sb.append("第").append(d).append("天：\n");
            sb.append("  上午：");
            if (actIdx < activities.size()) {
                sb.append(activities.get(actIdx++).getTitle());
            } else if (herIdx < heritages.size()) {
                sb.append(heritages.get(herIdx++).getName());
            }
            sb.append("\n  下午：");
            if (herIdx < heritages.size()) {
                sb.append(heritages.get(herIdx++).getName());
            } else if (actIdx < activities.size()) {
                sb.append(activities.get(actIdx++).getTitle());
            }
            sb.append("\n  晚上：自由活动，品尝当地美食\n\n");
        }
        sb.append("祝您旅途愉快！");
        return sb.toString();
    }

    private List<Activity> findActivitiesByDestination(String destination) {
        if ("全国".equals(destination)) {
            return activityRepository.findByStatusNotOrderByStartTimeAsc("ended");
        }
        return activityRepository.findByStatusNotOrderByStartTimeAsc("ended").stream()
                .filter(a -> a.getLocationCity() != null && a.getLocationCity().contains(destination))
                .collect(Collectors.toList());
    }

    private List<IntangibleCulturalHeritage> findHeritagesByDestination(String destination) {
        if ("全国".equals(destination)) {
            return intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc();
        }
        return intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc().stream()
                .filter(h -> h.getRegion() != null && h.getRegion().contains(destination))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildActivityCards(List<Activity> activities) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Activity a : activities) {
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("id", a.getId());
            card.put("title", a.getTitle());
            card.put("subtitle", a.getSubtitle());
            card.put("heritageType", a.getHeritageType());
            card.put("location", (a.getLocationCity() != null ? a.getLocationCity() : "") +
                    (a.getLocationDetail() != null ? " " + a.getLocationDetail() : ""));
            card.put("startTime", a.getStartTime() != null ? sdf.format(a.getStartTime()) : "");
            card.put("price", a.getPrice() != null ? a.getPrice() / 100.0 : 0);
            card.put("status", a.getStatus());
            cards.add(card);
        }
        return cards;
    }

    private List<Map<String, Object>> buildHeritageCards(List<IntangibleCulturalHeritage> heritages) {
        List<Map<String, Object>> cards = new ArrayList<>();
        for (IntangibleCulturalHeritage h : heritages) {
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("id", h.getId());
            card.put("name", h.getName());
            card.put("category", h.getCategory());
            card.put("region", h.getRegion());
            card.put("introduction", h.getIntroduction());
            cards.add(card);
        }
        return cards;
    }
}
