package com.yayfolk.backend.ai.vector;

import com.yayfolk.backend.ai.client.EmbeddingClient;
import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.DiscoverPost;
import com.yayfolk.backend.entity.IntangibleCulturalHeritage;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.DiscoverPostRepository;
import com.yayfolk.backend.repository.IntangibleCulturalHeritageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 向量索引构建器。
 * 从 AIResourceService.init() 提取，并新增 activities 索引（修复原有 BUG）。
 */
@Component
public class VectorIndexBuilder {

    private static final Logger logger = LoggerFactory.getLogger(VectorIndexBuilder.class);

    private final ActivityRepository activityRepository;
    private final DiscoverPostRepository discoverPostRepository;
    private final IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public VectorIndexBuilder(ActivityRepository activityRepository,
                              DiscoverPostRepository discoverPostRepository,
                              IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository,
                              EmbeddingClient embeddingClient,
                              VectorStore vectorStore) {
        this.activityRepository = activityRepository;
        this.discoverPostRepository = discoverPostRepository;
        this.intangibleCulturalHeritageRepository = intangibleCulturalHeritageRepository;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 应用启动时自动构建索引（含 posts + heritages + activities）。
     */
    @PostConstruct
    public void init() {
        buildIndex();
    }

    /**
     * 构建完整的向量索引（posts + heritages + activities）。
     */
    public void buildIndex() {
        logger.info("Starting to build vector index...");
        vectorStore.clear();

        int postCount = indexPosts();
        int heritageCount = indexHeritages();
        int activityCount = indexActivities();

        vectorStore.setIndexed(true);
        logger.info("Vector index built: {} posts, {} heritages, {} activities, embedding available: {}",
                postCount, heritageCount, activityCount, embeddingClient.isAvailable());
    }

    private int indexPosts() {
        List<DiscoverPost> posts = discoverPostRepository.findByStatusAndAuditStatusOrderByCreateTimeDesc(1, "passed");
        logger.info("Found {} approved posts to index", posts.size());

        List<String> texts = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();

        for (DiscoverPost post : posts) {
            texts.add(buildPostText(post));
            ids.add("post_" + post.getId());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", post.getTitle());
            metadata.put("category", post.getCategory());
            metadata.put("tags", post.getTags());
            metadatas.add(metadata);
        }

        addEntriesToStore("post", texts, ids, metadatas, posts);
        return posts.size();
    }

    private int indexHeritages() {
        List<IntangibleCulturalHeritage> heritages = intangibleCulturalHeritageRepository
                .findAllByOrderByIsFeaturedDescViewCountDescIdAsc();
        logger.info("Found {} heritages to index", heritages.size());

        List<String> texts = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();

        for (IntangibleCulturalHeritage heritage : heritages) {
            texts.add(buildHeritageText(heritage));
            ids.add("heritage_" + heritage.getId());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("name", heritage.getName());
            metadata.put("category", heritage.getCategory());
            metadata.put("region", heritage.getRegion());
            metadatas.add(metadata);
        }

        addEntriesToStore("heritage", texts, ids, metadatas, heritages);
        return heritages.size();
    }

    /**
     * 新增：索引 activities。修复原 AIResourceService 中搜 "activity" 类型永远无结果的 BUG。
     */
    private int indexActivities() {
        List<Activity> activities = activityRepository.findByStatusNotOrderByStartTimeAsc("ended");
        logger.info("Found {} activities to index", activities.size());

        List<String> texts = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();

        for (Activity activity : activities) {
            texts.add(buildActivityText(activity));
            ids.add("activity_" + activity.getId());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", activity.getTitle());
            metadata.put("heritageType", activity.getHeritageType());
            metadata.put("locationCity", activity.getLocationCity());
            metadata.put("price", activity.getPrice());
            metadatas.add(metadata);
        }

        addEntriesToStore("activity", texts, ids, metadatas, activities);
        return activities.size();
    }

    private void addEntriesToStore(String type, List<String> texts, List<String> ids,
                                    List<Map<String, Object>> metadatas,
                                    List<?> entities) {
        if (embeddingClient.isAvailable()) {
            List<float[]> embeddings = embeddingClient.getEmbeddings(texts);
            for (int i = 0; i < entities.size(); i++) {
                vectorStore.addEntry(ids.get(i), type, ((Number) getEntityId(entities.get(i))).longValue(),
                        texts.get(i), embeddings.get(i), metadatas.get(i));
            }
        } else {
            for (int i = 0; i < entities.size(); i++) {
                vectorStore.addEntry(ids.get(i), type, ((Number) getEntityId(entities.get(i))).longValue(),
                        texts.get(i), null, metadatas.get(i));
            }
        }
    }

    // ==================== 实时增量索引 ====================

    /**
     * 实时索引单条帖子（创建/更新时调用）。
     */
    public void indexPost(DiscoverPost post) {
        if (post.getStatus() == null || post.getStatus() != 1) return;
        if (!"passed".equals(post.getAuditStatus())) return;

        String id = "post_" + post.getId();
        String text = buildPostText(post);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", post.getTitle());
        metadata.put("category", post.getCategory());
        metadata.put("tags", post.getTags());

        float[] embedding = null;
        if (embeddingClient.isAvailable()) {
            try {
                List<float[]> embeddings = embeddingClient.getEmbeddings(Collections.singletonList(text));
                if (embeddings != null && !embeddings.isEmpty()) {
                    embedding = embeddings.get(0);
                }
            } catch (Exception e) {
                logger.warn("Failed to get embedding for post {}, indexing without vector", post.getId(), e);
            }
        }
        vectorStore.addEntry(id, "post", post.getId(), text, embedding, metadata);
        logger.info("Indexed post {}: {}", post.getId(), post.getTitle());
    }

    /**
     * 实时索引单条非遗（创建/更新时调用）。
     */
    public void indexHeritage(IntangibleCulturalHeritage heritage) {
        String id = "heritage_" + heritage.getId();
        String text = buildHeritageText(heritage);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", heritage.getName());
        metadata.put("category", heritage.getCategory());
        metadata.put("region", heritage.getRegion());

        float[] embedding = null;
        if (embeddingClient.isAvailable()) {
            try {
                List<float[]> embeddings = embeddingClient.getEmbeddings(Collections.singletonList(text));
                if (embeddings != null && !embeddings.isEmpty()) {
                    embedding = embeddings.get(0);
                }
            } catch (Exception e) {
                logger.warn("Failed to get embedding for heritage {}, indexing without vector", heritage.getId(), e);
            }
        }
        vectorStore.addEntry(id, "heritage", heritage.getId(), text, embedding, metadata);
        logger.info("Indexed heritage {}: {}", heritage.getId(), heritage.getName());
    }

    /**
     * 从索引中移除单条帖子。
     */
    public void removePostFromIndex(Long postId) {
        vectorStore.removeEntry("post_" + postId);
        logger.info("Removed post {} from index", postId);
    }

    /**
     * 从索引中移除单条非遗。
     */
    public void removeHeritageFromIndex(Long heritageId) {
        vectorStore.removeEntry("heritage_" + heritageId);
        logger.info("Removed heritage {} from index", heritageId);
    }

    private Object getEntityId(Object entity) {
        try {
            return entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get entity ID", e);
        }
    }

    private String buildPostText(DiscoverPost post) {
        StringBuilder sb = new StringBuilder();
        sb.append(post.getTitle());
        if (post.getCategory() != null) {
            sb.append(" ").append(post.getCategory());
        }
        if (post.getTags() != null) {
            sb.append(" ").append(post.getTags().replace("[", "").replace("]", "").replace("\"", ""));
        }
        if (post.getContent() != null) {
            String content = post.getContent();
            if (content.length() > 500) {
                content = content.substring(0, 500);
            }
            sb.append(" ").append(content);
        }
        return sb.toString();
    }

    private String buildHeritageText(IntangibleCulturalHeritage heritage) {
        StringBuilder sb = new StringBuilder();
        sb.append(heritage.getName());
        if (heritage.getCategory() != null) sb.append(" ").append(heritage.getCategory());
        if (heritage.getSubcategory() != null) sb.append(" ").append(heritage.getSubcategory());
        if (heritage.getRegion() != null) sb.append(" ").append(heritage.getRegion());
        if (heritage.getDynasty() != null) sb.append(" ").append(heritage.getDynasty());
        if (heritage.getIntroduction() != null) sb.append(" ").append(heritage.getIntroduction());
        if (heritage.getHistory() != null) {
            String history = heritage.getHistory();
            if (history.length() > 300) history = history.substring(0, 300);
            sb.append(" ").append(history);
        }
        return sb.toString();
    }

    private String buildActivityText(Activity activity) {
        StringBuilder sb = new StringBuilder();
        sb.append(activity.getTitle());
        if (activity.getSubtitle() != null) sb.append(" ").append(activity.getSubtitle());
        if (activity.getHeritageType() != null) sb.append(" ").append(activity.getHeritageType());
        if (activity.getLocationCity() != null) sb.append(" ").append(activity.getLocationCity());
        if (activity.getLocationProvince() != null) sb.append(" ").append(activity.getLocationProvince());
        if (activity.getContent() != null) {
            String content = activity.getContent();
            if (content.length() > 500) content = content.substring(0, 500);
            sb.append(" ").append(content);
        }
        return sb.toString();
    }
}
