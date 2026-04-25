package com.yayfolk.backend.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QueryIntentClassifierTest {
    
    @Test
    public void testStructuredQuery() {
        // 测试结构化查询
        assertEquals(QueryIntentClassifier.Intent.STRUCTURED_QUERY, 
                     QueryIntentClassifier.classify("下周六有什么非遗活动？"));
        assertEquals(QueryIntentClassifier.Intent.STRUCTURED_QUERY, 
                     QueryIntentClassifier.classify("北京有哪些免费的非遗活动？"));
        assertEquals(QueryIntentClassifier.Intent.STRUCTURED_QUERY, 
                     QueryIntentClassifier.classify("什么时候有刺绣活动？"));
        assertEquals(QueryIntentClassifier.Intent.STRUCTURED_QUERY, 
                     QueryIntentClassifier.classify("在哪里可以参加非遗活动？"));
        assertEquals(QueryIntentClassifier.Intent.STRUCTURED_QUERY, 
                     QueryIntentClassifier.classify("活动多少钱？"));
    }
    
    @Test
    public void testItineraryPlanning() {
        // 测试行程规划
        assertEquals(QueryIntentClassifier.Intent.ITINERARY_PLANNING, 
                     QueryIntentClassifier.classify("帮我规划3天2日北京非遗之旅"));
        assertEquals(QueryIntentClassifier.Intent.ITINERARY_PLANNING, 
                     QueryIntentClassifier.classify("上海2天1晚非遗行程"));
        assertEquals(QueryIntentClassifier.Intent.ITINERARY_PLANNING, 
                     QueryIntentClassifier.classify("推荐一下苏州的非遗路线"));
        assertEquals(QueryIntentClassifier.Intent.ITINERARY_PLANNING, 
                     QueryIntentClassifier.classify("怎么安排杭州的非遗之旅？"));
        assertEquals(QueryIntentClassifier.Intent.ITINERARY_PLANNING, 
                     QueryIntentClassifier.classify("北京5天非遗攻略"));
    }
    
    @Test
    public void testKnowledgeQA() {
        // 测试知识问答
        assertEquals(QueryIntentClassifier.Intent.KNOWLEDGE_QA, 
                     QueryIntentClassifier.classify("给我几篇科普非遗服饰的帖子"));
        assertEquals(QueryIntentClassifier.Intent.KNOWLEDGE_QA, 
                     QueryIntentClassifier.classify("非遗项目有哪些？"));
        assertEquals(QueryIntentClassifier.Intent.KNOWLEDGE_QA, 
                     QueryIntentClassifier.classify("什么是非物质文化遗产？"));
        assertEquals(QueryIntentClassifier.Intent.KNOWLEDGE_QA, 
                     QueryIntentClassifier.classify("介绍一下京剧"));
        assertEquals(QueryIntentClassifier.Intent.KNOWLEDGE_QA, 
                     QueryIntentClassifier.classify("非遗保护的意义"));
    }
    
    @Test
    public void testEmptyQuery() {
        // 测试空查询
        assertEquals(QueryIntentClassifier.Intent.KNOWLEDGE_QA, 
                     QueryIntentClassifier.classify(""));
        assertEquals(QueryIntentClassifier.Intent.KNOWLEDGE_QA, 
                     QueryIntentClassifier.classify(null));
    }
}