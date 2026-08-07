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
 */
@Component
public class SearchPostsHeritagesTool implements Tool {

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

        // 向量搜索帖子和非遗
        List<VectorStore.SearchResult> postResults = vectorStore.search(query, "post", limit);
        List<VectorStore.SearchResult> heritageResults = vectorStore.search(query, "heritage", limit);

        // 解析帖子
        List<Map<String, Object>> posts = new ArrayList<>();
        Set<Long> seenPostIds = new HashSet<>();
        for (VectorStore.SearchResult sr : postResults) {
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

        // 解析非遗
        List<Map<String, Object>> heritages = new ArrayList<>();
        Set<Long> seenHeritageIds = new HashSet<>();
        for (VectorStore.SearchResult sr : heritageResults) {
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

        // 无结果时不返回热门内容，避免无关推荐
        // 空结果由 Agent 的 synthesizeResponse 统一处理，给出友好提示

        Map<String, Object> data = new HashMap<>();
        data.put("posts", posts);
        data.put("heritages", heritages);

        String summary = String.format("找到%d篇相关帖子和%d个相关非遗项目", posts.size(), heritages.size());
        return ToolResult.success(summary, data);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}
