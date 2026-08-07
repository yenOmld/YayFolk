package com.yayfolk.backend.agent.intent;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 基于关键词规则的意图分类器（降级方案）。
 * 从原 QueryIntentClassifier 迁移，修复「推荐」→ RESOURCE_RECOMMEND。
 */
@Component
public class KeywordIntentClassifier implements IntentClassifier {

    private static final List<String> CITIES = Arrays.asList(
            "北京", "上海", "广州", "深圳", "杭州", "苏州", "成都", "西安", "南京", "武汉",
            "重庆", "天津", "长沙", "厦门", "青岛", "大连", "昆明", "丽江", "洛阳", "开封",
            "潮州", "景德镇", "福州", "泉州", "贵阳", "拉萨", "敦煌", "绍兴", "扬州"
    );

    private static final List<String> STRONG_PLANNING = Arrays.asList(
            "规划", "行程", "路线", "攻略", "推荐行程", "怎么玩", "之旅", "几日", "几晚"
    );

    private static final List<String> STRONG_RESOURCE = Arrays.asList(
            "活动", "报名", "价格", "费用", "多少钱", "什么时候", "在哪里", "哪天",
            "下周", "本周", "明天", "今天", "周末", "几月", "几号", "工作坊", "课程",
            "推荐", "有没有", "有什么", "有哪些", "帮我找", "搜索", "查找"
    );

    private static final List<String> WEAK_RESOURCE = Arrays.asList(
            "免费", "展览", "体验", "亲子", "元", "以下", "帖子", "项目"
    );

    private static final List<String> STRONG_KNOWLEDGE = Arrays.asList(
            "文章", "科普", "介绍", "历史", "工艺", "步骤", "特点", "是什么",
            "怎么做", "由来", "故事", "文化", "传承", "制作"
    );

    @Override
    public Intent classify(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Intent.KNOWLEDGE_QA;
        }

        // 闲谈/问候检测：优先判断，这类请求不需要调用任何工具
        if (isChitchat(query)) {
            return Intent.CHITCHAT;
        }

        int planningScore = 0;
        int resourceScore = 0;
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

        for (String kw : STRONG_RESOURCE) {
            if (query.contains(kw)) resourceScore += 3;
        }
        for (String kw : WEAK_RESOURCE) {
            if (query.contains(kw)) resourceScore += 1;
        }
        if (containsCity(query) && containsAny(query, WEAK_RESOURCE)) {
            resourceScore += 1;
        }

        for (String kw : STRONG_KNOWLEDGE) {
            if (query.contains(kw)) knowledgeScore += 3;
        }
        if (!containsCity(query) && !containsAny(query, STRONG_RESOURCE) && containsAny(query, Arrays.asList("是什么", "怎么", "历史", "工艺"))) {
            knowledgeScore += 2;
        }

        if (planningScore > resourceScore && planningScore > knowledgeScore) {
            return Intent.ITINERARY_PLANNING;
        } else if (resourceScore > knowledgeScore) {
            return Intent.RESOURCE_RECOMMEND;
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

    /**
     * 判断是否为闲谈/打招呼，这类消息不需要调用工具搜索。
     */
    private static boolean isChitchat(String query) {
        // 纯粹问候
        String[] greetings = {"你好", "嗨", "哈喽", "早上好", "下午好", "晚上好", "hi", "hello", "hey"};
        for (String g : greetings) {
            if (query.equalsIgnoreCase(g) || query.startsWith(g + " ") || query.startsWith(g + "！")
                    || query.startsWith(g + "!") || query.startsWith(g + "~") || query.equals(g + "！")) {
                return true;
            }
        }

        // 询问身份/自我介绍
        String[] selfIntro = {"你是谁", "你叫什么", "你的名字", "介绍一下你自己", "你是什么",
                "你是做什么的", "你是哪位", "你叫什么名字", "介绍下自己"};
        for (String s : selfIntro) {
            if (query.contains(s)) return true;
        }

        // 询问能力
        String[] capability = {"你能做什么", "你有什么功能", "你会什么", "你能干嘛",
                "你能帮我做什么", "你能干什么", "你有什么用"};
        for (String c : capability) {
            if (query.contains(c)) return true;
        }

        // 感谢
        String[] thanks = {"谢谢", "多谢", "感谢", "thank"};
        for (String t : thanks) {
            if (query.contains(t)) return true;
        }

        // 道别
        String[] goodbye = {"再见", "拜拜", "bye", "晚安", "回头见"};
        for (String g : goodbye) {
            if (query.contains(g)) return true;
        }

        // 纯粹感叹/无意义短句（≤2个字符且不包含非遗相关内容）
        if (query.length() <= 2) {
            String[] gibberish = {"嗯", "哦", "啊", "好", "行", "对", "是的", "不是", "在吗", "在不在",
                    "干嘛", "怎么", "哈哈", "嘿嘿", "emm", "...", "。。。", "？？", "!!", "ok"};
            for (String g : gibberish) {
                if (query.equalsIgnoreCase(g)) return true;
            }
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
