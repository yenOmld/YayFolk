package com.yayfolk.backend.ai.vector;

import java.util.List;
import java.util.Map;

/**
 * 向量存储接口，抽象语义搜索能力。
 */
public interface VectorStore {

    /**
     * 清空所有索引条目。
     */
    void clear();

    /**
     * 添加一条向量条目。
     */
    void addEntry(String id, String type, Long entityId, String text, float[] embedding, Map<String, Object> metadata);

    /**
     * 删除一条向量条目。
     */
    void removeEntry(String id);

    /**
     * 按查询文本和类型搜索 topK 条结果。
     * @param query 查询文本
     * @param type  类型过滤（"post", "heritage", "activity"），null 表示不过滤
     * @param topK  返回结果数
     */
    List<SearchResult> search(String query, String type, int topK);

    /**
     * 指定类型的条目数量。
     */
    int sizeByType(String type);

    /**
     * 总条目数。
     */
    int size();

    /**
     * 索引是否已构建完成。
     */
    boolean isIndexed();

    /**
     * 设置索引完成标记。
     */
    void setIndexed(boolean indexed);

    /**
     * 搜索结果：包含匹配条目和相似度分数。
     */
    class SearchResult {
        private final VectorEntry entry;
        private final double score;

        public SearchResult(VectorEntry entry, double score) {
            this.entry = entry;
            this.score = score;
        }

        public VectorEntry getEntry() { return entry; }
        public double getScore() { return score; }
    }

    /**
     * 向量条目。
     */
    class VectorEntry {
        private final String id;
        private final String type;
        private final Long entityId;
        private final String text;
        private final float[] embedding;
        private final Map<String, Object> metadata;

        public VectorEntry(String id, String type, Long entityId, String text,
                           float[] embedding, Map<String, Object> metadata) {
            this.id = id;
            this.type = type;
            this.entityId = entityId;
            this.text = text;
            this.embedding = embedding;
            this.metadata = metadata;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public Long getEntityId() { return entityId; }
        public String getText() { return text; }
        public float[] getEmbedding() { return embedding; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}
