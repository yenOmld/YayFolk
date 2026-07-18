package com.yayfolk.backend.agent.tool;

import com.yayfolk.backend.ai.vector.VectorStore;
import com.yayfolk.backend.entity.DiscoverPost;
import com.yayfolk.backend.entity.IntangibleCulturalHeritage;
import com.yayfolk.backend.repository.DiscoverPostRepository;
import com.yayfolk.backend.repository.IntangibleCulturalHeritageRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 向量搜索工具：搜索相关帖子和非遗项目。
 *
 * 支持 search_type 参数控制搜索范围：
 * - "posts": 只搜帖子
 * - "heritages": 只搜非遗项目
 * - "both" / 默认: 两者都搜
 *
 * 内置二次分数过滤：帖子需 >= 0.55，非遗需 >= 0.45（比全局阈值更严格，避免无关结果）。
 */
@Component
public class SearchPostsHeritagesTool implements Tool {

    /** 帖子相关性阈值（比全局阈值更严格，帖子内容噪音大） */
    private static final double POST_SCORE_THRESHOLD = 0.55;
    /** 非遗项目相关性阈值 */
    private static final double HERITAGE_SCORE_THRESHOLD = 0.45;

    private final VectorStore vectorStore;
    private final DiscoverPostRepository discoverPostRepository;
    private final IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository;

    public SearchPostsHeritagesTool(VectorStore vectorStore,
                                     DiscoverPostRepository discoverPostRepository,
                                     IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository) {
        this.vectorStore = vectorStore;
        this.discoverPostRepository = discoverPostRepository;
        this.intangibleCulturalHeritageRepository = intangibleCulturalHeritageRepository;
    }

    @Override
    public String getName() {
        return "search_posts_heritages";
    }

    @Override
    public String getDescription() {
        return "搜索与查询相关的帖子和非遗项目。适合用户想了解非遗知识、查看相关帖子、搜索非遗项目等场景。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> queryProp = new LinkedHashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "搜索关键词或用户查询内容");
        properties.put("query", queryProp);

        Map<String, Object> limitProp = new LinkedHashMap<>();
        limitProp.put("type", "integer");
        limitProp.put("description", "返回结果数量上限，默认5");
        properties.put("limit", limitProp);

        Map<String, Object> searchTypeProp = new LinkedHashMap<>();
        searchTypeProp.put("type", "string");
        searchTypeProp.put("description", "搜索类型：posts(仅帖子)、heritages(仅非遗)、both(两者)，默认both");
        properties.put("search_type", searchTypeProp);

        schema.put("properties", properties);
        schema.put("required", Collections.singletonList("query"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String query = parameters.get("query") != null ? parameters.get("query").toString() : "";
        int limit = 5;
        if (parameters.get("limit") != null) {
            try {
                limit = Integer.parseInt(parameters.get("limit").toString());
            } catch (NumberFormatException ignored) {}
        }
        String searchType = parameters.get("search_type") != null
                ? parameters.get("search_type").toString() : "both";

        boolean searchPosts = "both".equals(searchType) || "posts".equals(searchType);
        boolean searchHeritages = "both".equals(searchType) || "heritages".equals(searchType);

        final List<Map<String, Object>> posts = new ArrayList<>();
        final List<Map<String, Object>> heritages = new ArrayList<>();

        // 搜索帖子（需要时）
        if (searchPosts) {
            List<VectorStore.SearchResult> postResults = vectorStore.search(query, "post", limit * 2);
            Set<Long> seenPostIds = new HashSet<>();
            for (VectorStore.SearchResult sr : postResults) {
                // 二次分数过滤：帖子内容噪音大，需要更高的相关性门槛
                if (sr.getScore() < POST_SCORE_THRESHOLD) continue;
                Long id = sr.getEntry().getEntityId();
                if (seenPostIds.add(id)) {
                    discoverPostRepository.findById(id).ifPresent(post -> {
                        Map<String, Object> card = new HashMap<>();
                        card.put("id", post.getId());
                        card.put("title", post.getTitle());
                        card.put("content", truncate(post.getContent(), 200));
                        card.put("score", sr.getScore());
                        posts.add(card);
                    });
                }
            }
        }

        // 搜索非遗项目（需要时）
        if (searchHeritages) {
            List<VectorStore.SearchResult> heritageResults = vectorStore.search(query, "heritage", limit * 2);
            Set<Long> seenHeritageIds = new HashSet<>();
            for (VectorStore.SearchResult sr : heritageResults) {
                if (sr.getScore() < HERITAGE_SCORE_THRESHOLD) continue;
                Long id = sr.getEntry().getEntityId();
                if (seenHeritageIds.add(id)) {
                    intangibleCulturalHeritageRepository.findById(id).ifPresent(heritage -> {
                        Map<String, Object> card = new HashMap<>();
                        card.put("id", heritage.getId());
                        card.put("name", heritage.getName());
                        card.put("category", heritage.getCategory());
                        card.put("region", heritage.getRegion());
                        card.put("introduction", truncate(heritage.getIntroduction(), 200));
                        card.put("score", sr.getScore());
                        heritages.add(card);
                    });
                }
            }
        }

        // 截断到请求数量
        List<Map<String, Object>> finalPosts = posts.size() > limit
                ? new ArrayList<>(posts.subList(0, limit)) : posts;
        List<Map<String, Object>> finalHeritages = heritages.size() > limit
                ? new ArrayList<>(heritages.subList(0, limit)) : heritages;

        // 无结果时不返回热门内容，避免无关推荐
        // 空结果由 Agent 的 synthesizeResponse 统一处理

        Map<String, Object> data = new HashMap<>();
        data.put("posts", finalPosts);
        data.put("heritages", finalHeritages);

        String summary = String.format("找到%d篇相关帖子和%d个相关非遗项目", finalPosts.size(), finalHeritages.size());
        return ToolResult.success(summary, data);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}
