package com.yayfolk.backend.service;

import com.yayfolk.backend.ai.vector.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 ConcurrentHashMap 的内存向量存储实现。
 * 支持向量搜索（余弦相似度）和关键词降级搜索。
 */
@Component
public class InMemoryVectorStore implements VectorStore {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryVectorStore.class);

    private final EmbeddingService embeddingService;
    private final Map<String, VectorEntry> store = new ConcurrentHashMap<>();
    private volatile boolean indexed = false;

    public InMemoryVectorStore(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public void clear() {
        store.clear();
        indexed = false;
    }

    @Override
    public void addEntry(String id, String type, Long entityId, String text,
                         float[] embedding, Map<String, Object> metadata) {
        store.put(id, new VectorEntry(id, type, entityId, text, embedding, metadata));
    }

    @Override
    public void removeEntry(String id) {
        store.remove(id);
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public int sizeByType(String type) {
        return (int) store.values().stream().filter(e -> type.equals(e.getType())).count();
    }

    @Override
    public boolean isIndexed() {
        return indexed;
    }

    @Override
    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    @Override
    public List<SearchResult> search(String query, String type, int topK) {
        float[] queryEmbedding = embeddingService.getEmbedding(query);

        if (queryEmbedding != null) {
            return searchByVector(query, queryEmbedding, type, topK);
        } else {
            return searchByKeyword(query, type, topK);
        }
    }

    private List<SearchResult> searchByVector(String query, float[] queryEmbedding, String type, int topK) {
        List<SearchResult> results = new ArrayList<>();
        double threshold = EmbeddingService.getSimilarityThreshold();

        for (VectorEntry entry : store.values()) {
            if (type != null && !type.equals(entry.getType())) {
                continue;
            }
            if (entry.getEmbedding() == null) {
                double keywordScore = computeKeywordScore(query, entry.getText(), entry.getMetadata());
                if (keywordScore >= threshold) {
                    results.add(new SearchResult(entry, keywordScore));
                }
                continue;
            }

            double similarity = EmbeddingService.cosineSimilarity(queryEmbedding, entry.getEmbedding());
            if (similarity >= threshold) {
                results.add(new SearchResult(entry, similarity));
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    private List<SearchResult> searchByKeyword(String query, String type, int topK) {
        List<SearchResult> results = new ArrayList<>();

        for (VectorEntry entry : store.values()) {
            if (type != null && !type.equals(entry.getType())) {
                continue;
            }

            double score = computeKeywordScore(query, entry.getText(), entry.getMetadata());
            if (score > 0) {
                results.add(new SearchResult(entry, score));
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    private double computeKeywordScore(String query, String text, Map<String, Object> metadata) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        String lowerQuery = query.toLowerCase();
        String lowerText = text.toLowerCase();

        Set<String> queryWords = new HashSet<>(segmentChinese(lowerQuery));
        Set<String> textWords = new HashSet<>(segmentChinese(lowerText));

        if (queryWords.isEmpty()) {
            return 0.0;
        }

        int matchCount = 0;
        for (String word : queryWords) {
            if (textWords.contains(word) || lowerText.contains(word)) {
                matchCount++;
            }
        }

        if (metadata != null) {
            for (Map.Entry<String, Object> meta : metadata.entrySet()) {
                if (meta.getValue() instanceof String) {
                    String metaValue = ((String) meta.getValue()).toLowerCase();
                    for (String word : queryWords) {
                        if (metaValue.contains(word)) {
                            matchCount++;
                        }
                    }
                }
            }
        }

        return (double) matchCount / queryWords.size();
    }

    private List<String> segmentChinese(String text) {
        List<String> words = new ArrayList<>();

        for (int i = 0; i < text.length() - 1; i++) {
            words.add(text.substring(i, i + 2));
        }
        for (int i = 0; i < text.length() - 2; i++) {
            words.add(text.substring(i, i + 3));
        }

        String[] singleChars = text.split("");
        for (String c : singleChars) {
            if (c.trim().length() > 0) {
                words.add(c);
            }
        }

        return words;
    }
}
