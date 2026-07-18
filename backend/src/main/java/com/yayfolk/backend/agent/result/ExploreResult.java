package com.yayfolk.backend.agent.result;

import com.yayfolk.backend.agent.intent.Intent;
import java.util.*;

/**
 * 探索资源统一结果模型。替代 AIResourceService 中的 Map<String, Object> 返回值。
 */
public class ExploreResult {

    private Intent intent;
    private String userQuery;
    private String answerText;
    private List<Map<String, Object>> activityCards = new ArrayList<>();
    private List<Map<String, Object>> heritageCards = new ArrayList<>();
    private List<Map<String, Object>> postCards = new ArrayList<>();
    private String itineraryText;
    private int total;
    private Map<String, Object> metadata = new HashMap<>();

    public static ExploreResult of(Intent intent, String userQuery) {
        ExploreResult result = new ExploreResult();
        result.intent = intent;
        result.userQuery = userQuery;
        return result;
    }

    // Getters and setters

    public Intent getIntent() { return intent; }
    public void setIntent(Intent intent) { this.intent = intent; }

    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public List<Map<String, Object>> getActivityCards() { return activityCards; }
    public void setActivityCards(List<Map<String, Object>> activityCards) { this.activityCards = activityCards; }

    public List<Map<String, Object>> getHeritageCards() { return heritageCards; }
    public void setHeritageCards(List<Map<String, Object>> heritageCards) { this.heritageCards = heritageCards; }

    public List<Map<String, Object>> getPostCards() { return postCards; }
    public void setPostCards(List<Map<String, Object>> postCards) { this.postCards = postCards; }

    public String getItineraryText() { return itineraryText; }
    public void setItineraryText(String itineraryText) { this.itineraryText = itineraryText; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    /**
     * 生成给用户看的摘要文本。
     * 优先使用 LLM 生成的 answerText，降级时使用模板化摘要。
     */
    public String buildSummary() {
        // 如果有 LLM 生成的回复，直接使用
        if (answerText != null && !answerText.isEmpty()) {
            return answerText;
        }

        switch (intent) {
            case CHITCHAT:
                return answerText != null ? answerText : "你好！有什么可以帮你的吗？";
            case ITINERARY_PLANNING:
                return "为您规划了" + metadata.getOrDefault("days", "?") + "天的"
                        + metadata.getOrDefault("destination", "") + "非遗之旅";
            case RESOURCE_RECOMMEND:
                return total > 0 ? "为您找到 " + total + " 个相关活动" : "暂无符合条件的活动";
            case KNOWLEDGE_QA:
                return postCards.isEmpty() && heritageCards.isEmpty()
                        ? "关于这个问题，平台暂未收录相关资料，请在社区提问或尝试其他关键词。"
                        : "为您找到 " + postCards.size() + " 篇相关帖子和 "
                        + heritageCards.size() + " 个相关非遗项目";
            default:
                return "查询完成";
        }
    }
}
