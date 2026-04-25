package com.yayfolk.backend.service;

import java.util.Arrays;
import java.util.List;

public class QueryIntentClassifier {

    public enum Intent {
        STRUCTURED_QUERY,
        ITINERARY_PLANNING,
        KNOWLEDGE_QA
    }

    private static final List<String> CITIES = Arrays.asList(
            "北京", "上海", "广州", "深圳", "杭州", "苏州", "成都", "西安", "南京", "武汉",
            "重庆", "天津", "长沙", "厦门", "青岛", "大连", "昆明", "丽江", "洛阳", "开封",
            "潮州", "景德镇", "福州", "泉州", "贵阳", "昆明", "拉萨", "敦煌", "绍兴", "扬州"
    );

    private static final List<String> STRONG_PLANNING = Arrays.asList(
            "规划", "行程", "路线", "攻略", "推荐行程", "怎么玩", "之旅", "几日", "几晚"
    );

    private static final List<String> STRONG_STRUCTURED = Arrays.asList(
            "活动", "报名", "价格", "费用", "多少钱", "什么时候", "在哪里", "哪天",
            "下周", "本周", "明天", "今天", "周末", "几月", "几号", "工作坊", "课程"
    );

    private static final List<String> WEAK_STRUCTURED = Arrays.asList(
            "免费", "展览", "体验", "亲子", "元", "以下", "有什么", "有没有", "有哪些"
    );

    private static final List<String> STRONG_KNOWLEDGE = Arrays.asList(
            "帖子", "文章", "科普", "介绍", "历史", "工艺", "步骤", "特点", "是什么",
            "怎么做", "由来", "故事", "文化", "传承", "制作", "推荐", "项目"
    );

    public static Intent classify(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Intent.KNOWLEDGE_QA;
        }

        int planningScore = 0;
        int structuredScore = 0;
        int knowledgeScore = 0;

        for (String kw : STRONG_PLANNING) {
            if (query.contains(kw)) planningScore += 3;
        }
        if (query.contains("天") && (query.contains("几") || query.matches(".*\\d+天.*") || query.contains("两"))) {
            planningScore += 3;
        }
        if (query.contains("去") && containsCity(query)) {
            planningScore += 2;
        }
        if (query.contains("游") && containsCity(query)) {
            planningScore += 2;
        }
        if (query.contains("安排") && containsCity(query)) {
            planningScore += 2;
        }

        for (String kw : STRONG_STRUCTURED) {
            if (query.contains(kw)) structuredScore += 3;
        }
        for (String kw : WEAK_STRUCTURED) {
            if (query.contains(kw)) structuredScore += 1;
        }
        if (containsCity(query) && (containsAny(query, WEAK_STRUCTURED) || containsAny(query, STRONG_STRUCTURED))) {
            structuredScore += 1;
        }

        for (String kw : STRONG_KNOWLEDGE) {
            if (query.contains(kw)) knowledgeScore += 3;
        }
        if (!containsCity(query) && !containsAny(query, STRONG_STRUCTURED) && containsAny(query, WEAK_STRUCTURED)) {
            knowledgeScore += 1;
        }

        if (planningScore > structuredScore && planningScore > knowledgeScore) {
            return Intent.ITINERARY_PLANNING;
        } else if (structuredScore > knowledgeScore) {
            return Intent.STRUCTURED_QUERY;
        } else {
            return Intent.KNOWLEDGE_QA;
        }
    }

    private static boolean containsCity(String query) {
        for (String city : CITIES) {
            if (query.contains(city)) return true;
        }
        return false;
    }

    private static boolean containsAny(String query, List<String> keywords) {
        for (String keyword : keywords) {
            if (query.contains(keyword)) return true;
        }
        return false;
    }
}
