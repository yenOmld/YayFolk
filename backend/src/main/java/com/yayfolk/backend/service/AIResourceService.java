package com.yayfolk.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.DiscoverPost;
import com.yayfolk.backend.entity.IntangibleCulturalHeritage;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.DiscoverPostRepository;
import com.yayfolk.backend.repository.IntangibleCulturalHeritageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AIResourceService {

    private static final Logger logger = LoggerFactory.getLogger(AIResourceService.class);

    private final ActivityRepository activityRepository;
    private final IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository;
    private final DiscoverPostRepository discoverPostRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final InMemoryVectorStore vectorStore;

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${embedding.api.key:}")
    private String embeddingApiKey;

    @Value("${embedding.api.url:https://api.siliconflow.cn/v1/embeddings}")
    private String embeddingApiUrl;

    @Value("${embedding.api.model:BAAI/bge-large-zh-v1.5}")
    private String embeddingModel;

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList(
            "heritage_type", "location_city", "price", "status"
    ));

    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "drop", "delete", "update", "insert", "alter", "truncate", "exec", "execute", "grant", "revoke"
    ));

    public AIResourceService(ActivityRepository activityRepository,
                             IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository,
                             DiscoverPostRepository discoverPostRepository,
                             RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             EmbeddingService embeddingService,
                             InMemoryVectorStore vectorStore) {
        this.activityRepository = activityRepository;
        this.intangibleCulturalHeritageRepository = intangibleCulturalHeritageRepository;
        this.discoverPostRepository = discoverPostRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        embeddingService.configure(embeddingApiKey, embeddingApiUrl, embeddingModel);
        buildVectorIndex();
    }

    public void buildVectorIndex() {
        logger.info("Starting to build vector index...");
        vectorStore.clear();

        List<DiscoverPost> posts = discoverPostRepository.findByStatusAndAuditStatusOrderByCreateTimeDesc(1, "passed");
        logger.info("Found {} approved posts to index", posts.size());

        List<IntangibleCulturalHeritage> heritages = intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc();
        logger.info("Found {} heritages to index", heritages.size());

        List<String> postTexts = new ArrayList<>();
        List<String> postIds = new ArrayList<>();
        List<Map<String, Object>> postMetadatas = new ArrayList<>();

        for (DiscoverPost post : posts) {
            String text = buildPostText(post);
            String id = "post_" + post.getId();
            postTexts.add(text);
            postIds.add(id);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", post.getTitle());
            metadata.put("category", post.getCategory());
            metadata.put("tags", post.getTags());
            postMetadatas.add(metadata);
        }

        List<String> heritageTexts = new ArrayList<>();
        List<String> heritageIds = new ArrayList<>();
        List<Map<String, Object>> heritageMetadatas = new ArrayList<>();

        for (IntangibleCulturalHeritage heritage : heritages) {
            String text = buildHeritageText(heritage);
            String id = "heritage_" + heritage.getId();
            heritageTexts.add(text);
            heritageIds.add(id);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("name", heritage.getName());
            metadata.put("category", heritage.getCategory());
            metadata.put("region", heritage.getRegion());
            heritageMetadatas.add(metadata);
        }

        if (embeddingService.isEmbeddingAvailable()) {
            List<float[]> postEmbeddings = embeddingService.getEmbeddings(postTexts);
            for (int i = 0; i < posts.size(); i++) {
                vectorStore.addEntry(postIds.get(i), "post", posts.get(i).getId(),
                        postTexts.get(i), postEmbeddings.get(i), postMetadatas.get(i));
            }

            List<float[]> heritageEmbeddings = embeddingService.getEmbeddings(heritageTexts);
            for (int i = 0; i < heritages.size(); i++) {
                vectorStore.addEntry(heritageIds.get(i), "heritage", heritages.get(i).getId(),
                        heritageTexts.get(i), heritageEmbeddings.get(i), heritageMetadatas.get(i));
            }
        } else {
            for (int i = 0; i < posts.size(); i++) {
                vectorStore.addEntry(postIds.get(i), "post", posts.get(i).getId(),
                        postTexts.get(i), null, postMetadatas.get(i));
            }
            for (int i = 0; i < heritages.size(); i++) {
                vectorStore.addEntry(heritageIds.get(i), "heritage", heritages.get(i).getId(),
                        heritageTexts.get(i), null, heritageMetadatas.get(i));
            }
        }

        vectorStore.setIndexed(true);
        logger.info("Vector index built: {} posts, {} heritages, embedding available: {}",
                vectorStore.sizeByType("post"), vectorStore.sizeByType("heritage"),
                embeddingService.isEmbeddingAvailable());
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
        if (heritage.getCategory() != null) {
            sb.append(" ").append(heritage.getCategory());
        }
        if (heritage.getSubcategory() != null) {
            sb.append(" ").append(heritage.getSubcategory());
        }
        if (heritage.getRegion() != null) {
            sb.append(" ").append(heritage.getRegion());
        }
        if (heritage.getDynasty() != null) {
            sb.append(" ").append(heritage.getDynasty());
        }
        if (heritage.getIntroduction() != null) {
            sb.append(" ").append(heritage.getIntroduction());
        }
        if (heritage.getHistory() != null) {
            String history = heritage.getHistory();
            if (history.length() > 300) {
                history = history.substring(0, 300);
            }
            sb.append(" ").append(history);
        }
        return sb.toString();
    }

    public Map<String, Object> exploreResources(Map<String, Object> request) {
        String userInput = request.get("userInput") == null ? "" : request.get("userInput").toString().trim();
        if (userInput.isEmpty()) {
            throw new RuntimeException("请输入查询内容");
        }

        QueryIntentClassifier.Intent intent = QueryIntentClassifier.classify(userInput);

        switch (intent) {
            case STRUCTURED_QUERY:
                return handleStructuredQuery(userInput);
            case ITINERARY_PLANNING:
                return handleItineraryPlanning(userInput);
            case KNOWLEDGE_QA:
                return handleKnowledgeQA(userInput);
            default:
                return handleKnowledgeQA(userInput);
        }
    }

    private Map<String, Object> handleStructuredQuery(String userInput) {
        Map<String, Object> queryParams = extractQueryParams(userInput);
        String sql = buildSafeSQL(queryParams);
        List<Activity> activities = executeSafeSQLQuery(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("intent", "STRUCTURED_QUERY");
        result.put("query", userInput);
        result.put("sql", sql);
        result.put("activities", buildActivityCards(activities));
        result.put("total", activities.size());

        if (activities.isEmpty()) {
            result.put("message", generateEmptyResultMessage(userInput));
        }

        return result;
    }

    private Map<String, Object> handleItineraryPlanning(String userInput) {
        String destination = extractDestination(userInput);
        int days = extractDays(userInput);

        List<Activity> activities = findActivitiesByDestination(destination);
        List<IntangibleCulturalHeritage> heritages = findHeritagesByDestination(destination);

        List<Activity> selectedActivities = activities.stream().limit(days * 2L).collect(Collectors.toList());
        List<IntangibleCulturalHeritage> selectedHeritages = heritages.stream().limit(days * 2L).collect(Collectors.toList());

        String itineraryText = generateItineraryByLLM(userInput, destination, days, selectedActivities, selectedHeritages);

        Map<String, Object> result = new HashMap<>();
        result.put("intent", "ITINERARY_PLANNING");
        result.put("query", userInput);
        result.put("destination", destination);
        result.put("days", days);
        result.put("itinerary", itineraryText);
        result.put("activities", buildActivityCardsWithLinks(selectedActivities));
        result.put("heritages", buildHeritageCardsWithLinks(selectedHeritages));

        return result;
    }

    private String generateItineraryByLLM(String userInput, String destination, int days,
                                           List<Activity> activities, List<IntangibleCulturalHeritage> heritages) {
        StringBuilder context = new StringBuilder();
        context.append("目的地：").append(destination).append("\n");
        context.append("天数：").append(days).append("天\n\n");

        if (!activities.isEmpty()) {
            context.append("【可选活动】\n");
            for (Activity activity : activities) {
                context.append("- ID:").append(activity.getId())
                        .append(" 标题：").append(activity.getTitle());
                if (activity.getHeritageType() != null) {
                    context.append(" 非遗类型：").append(activity.getHeritageType());
                }
                if (activity.getLocationCity() != null) {
                    context.append(" 地点：").append(activity.getLocationCity());
                }
                if (activity.getPrice() != null) {
                    context.append(" 价格：").append(activity.getPrice() == 0 ? "免费" : activity.getPrice() + "元");
                }
                if (activity.getStartTime() != null) {
                    context.append(" 时间：").append(formatDateTime(activity.getStartTime()));
                }
                context.append("\n");
            }
        }

        if (!heritages.isEmpty()) {
            context.append("【可选非遗项目】\n");
            for (IntangibleCulturalHeritage heritage : heritages) {
                context.append("- ID:").append(heritage.getId())
                        .append(" 名称：").append(heritage.getName());
                if (heritage.getCategory() != null) {
                    context.append(" 类别：").append(heritage.getCategory());
                }
                if (heritage.getRegion() != null) {
                    context.append(" 地区：").append(heritage.getRegion());
                }
                if (heritage.getIntroduction() != null) {
                    String intro = heritage.getIntroduction();
                    if (intro.length() > 100) {
                        intro = intro.substring(0, 100) + "...";
                    }
                    context.append(" 简介：").append(intro);
                }
                context.append("\n");
            }
        }

        try {
            String systemPrompt = "你是一个专业的非遗旅行规划师。根据用户需求和提供的活动与非遗项目资源，生成详细的行程规划。\n" +
                    "规则：\n" +
                    "1. 按天规划，每天安排2-3个活动或项目，合理分配时间\n" +
                    "2. 考虑地理位置，尽量安排同一区域的活动在同一天\n" +
                    "3. 每天给出上午、下午、晚上的安排\n" +
                    "4. 在提到活动时，使用格式 [活动:ID:标题] 以便生成可点击链接\n" +
                    "5. 在提到非遗项目时，使用格式 [项目:ID:名称] 以便生成可点击链接\n" +
                    "6. 给出实用建议（交通、餐饮、注意事项）\n" +
                    "7. 语言简洁友好，不超过500字\n" +
                    "8. 不要使用Markdown格式，用纯文本\n";

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            Map<String, Object> contextMessage = new HashMap<>();
            contextMessage.put("role", "system");
            contextMessage.put("content", "可用资源：\n" + context.toString());
            messages.add(contextMessage);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userInput);
            messages.add(userMessage);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 800);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to generate itinerary by LLM", e);
        }

        return buildItineraryFallback(destination, days, activities, heritages);
    }

    private String buildItineraryFallback(String destination, int days,
                                           List<Activity> activities, List<IntangibleCulturalHeritage> heritages) {
        StringBuilder sb = new StringBuilder();
        sb.append("为您规划").append(days).append("天").append(destination).append("非遗之旅：\n\n");

        int actIdx = 0;
        int herIdx = 0;
        for (int d = 1; d <= days; d++) {
            sb.append("第").append(d).append("天：\n");
            sb.append("  上午：");
            if (actIdx < activities.size()) {
                Activity a = activities.get(actIdx++);
                sb.append("[活动:").append(a.getId()).append(":").append(a.getTitle()).append("]");
            } else if (herIdx < heritages.size()) {
                IntangibleCulturalHeritage h = heritages.get(herIdx++);
                sb.append("[项目:").append(h.getId()).append(":").append(h.getName()).append("]");
            }
            sb.append("\n  下午：");
            if (herIdx < heritages.size()) {
                IntangibleCulturalHeritage h = heritages.get(herIdx++);
                sb.append("[项目:").append(h.getId()).append(":").append(h.getName()).append("]");
            } else if (actIdx < activities.size()) {
                Activity a = activities.get(actIdx++);
                sb.append("[活动:").append(a.getId()).append(":").append(a.getTitle()).append("]");
            }
            sb.append("\n  晚上：自由活动，品尝当地美食\n\n");
        }

        sb.append("祝您旅途愉快！");
        return sb.toString();
    }

    private List<Map<String, Object>> buildActivityCardsWithLinks(List<Activity> activities) {
        return activities.stream().map(activity -> {
            Map<String, Object> card = new HashMap<>();
            card.put("id", activity.getId());
            card.put("title", activity.getTitle());
            card.put("subtitle", activity.getSubtitle());
            card.put("heritageType", activity.getHeritageType());
            card.put("location", activity.getLocationCity() + (activity.getLocationDetail() != null ? " " + activity.getLocationDetail() : ""));
            card.put("startTime", formatDateTime(activity.getStartTime()));
            card.put("price", activity.getPrice());
            card.put("status", activity.getStatus());
            card.put("link", "/activity/" + activity.getId());
            return card;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildHeritageCardsWithLinks(List<IntangibleCulturalHeritage> heritages) {
        return heritages.stream().map(heritage -> {
            Map<String, Object> card = new HashMap<>();
            card.put("id", heritage.getId());
            card.put("name", heritage.getName());
            card.put("category", heritage.getCategory());
            card.put("region", heritage.getRegion());
            card.put("introduction", heritage.getIntroduction());
            card.put("link", "/home/heritage?highlight=" + heritage.getId());
            return card;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> handleKnowledgeQA(String userInput) {
        List<InMemoryVectorStore.SearchResult> postResults = vectorStore.search(userInput, "post", 3);
        List<InMemoryVectorStore.SearchResult> heritageResults = vectorStore.search(userInput, "heritage", 3);

        List<DiscoverPost> matchedPosts = new ArrayList<>();
        List<Long> matchedPostIds = new ArrayList<>();
        for (InMemoryVectorStore.SearchResult sr : postResults) {
            Long entityId = sr.getEntry().getEntityId();
            if (!matchedPostIds.contains(entityId)) {
                matchedPostIds.add(entityId);
                discoverPostRepository.findById(entityId).ifPresent(matchedPosts::add);
            }
        }

        List<IntangibleCulturalHeritage> matchedHeritages = new ArrayList<>();
        List<Long> matchedHeritageIds = new ArrayList<>();
        for (InMemoryVectorStore.SearchResult sr : heritageResults) {
            Long entityId = sr.getEntry().getEntityId();
            if (!matchedHeritageIds.contains(entityId)) {
                matchedHeritageIds.add(entityId);
                intangibleCulturalHeritageRepository.findById(entityId).ifPresent(matchedHeritages::add);
            }
        }

        if (matchedPosts.isEmpty() && matchedHeritages.isEmpty()) {
            List<DiscoverPost> hotPosts = discoverPostRepository.findByStatusAndAuditStatusOrderByCreateTimeDesc(1, "passed");
            matchedPosts = hotPosts.stream().limit(3).collect(Collectors.toList());
            List<IntangibleCulturalHeritage> hotHeritages = intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc();
            matchedHeritages = hotHeritages.stream().limit(3).collect(Collectors.toList());
        }

        String answer = generateKnowledgeAnswer(userInput, matchedPosts, matchedHeritages);

        Map<String, Object> result = new HashMap<>();
        result.put("intent", "KNOWLEDGE_QA");
        result.put("query", userInput);
        result.put("answer", answer);
        result.put("posts", buildPostCards(matchedPosts));
        result.put("heritages", buildHeritageCards(matchedHeritages));
        result.put("totalPosts", matchedPosts.size());
        result.put("totalHeritages", matchedHeritages.size());

        return result;
    }

    private String generateKnowledgeAnswer(String userInput, List<DiscoverPost> posts, List<IntangibleCulturalHeritage> heritages) {
        StringBuilder context = new StringBuilder();

        if (!posts.isEmpty()) {
            context.append("【相关帖子】\n");
            for (DiscoverPost post : posts) {
                context.append("- 标题：").append(post.getTitle());
                if (post.getContent() != null) {
                    String content = post.getContent();
                    if (content.length() > 300) {
                        content = content.substring(0, 300) + "...";
                    }
                    context.append("\n  内容：").append(content);
                }
                context.append("\n");
            }
        }

        if (!heritages.isEmpty()) {
            context.append("【相关非遗项目】\n");
            for (IntangibleCulturalHeritage heritage : heritages) {
                context.append("- 名称：").append(heritage.getName());
                if (heritage.getCategory() != null) {
                    context.append("（").append(heritage.getCategory()).append("）");
                }
                if (heritage.getRegion() != null) {
                    context.append(" 地区：").append(heritage.getRegion());
                }
                if (heritage.getIntroduction() != null) {
                    String intro = heritage.getIntroduction();
                    if (intro.length() > 200) {
                        intro = intro.substring(0, 200) + "...";
                    }
                    context.append("\n  简介：").append(intro);
                }
                context.append("\n");
            }
        }

        if (context.length() == 0) {
            return "抱歉，没有找到与您问题相关的非遗内容。您可以尝试换个关键词搜索，比如「刺绣」「京剧」「剪纸」等。";
        }

        try {
            String systemPrompt = "你是一个非遗文化知识助手。根据提供的参考资料，用简洁友好的中文回答用户的问题。\n" +
                    "规则：\n" +
                    "1. 基于参考资料回答，不要编造信息\n" +
                    "2. 如果参考资料与问题不相关，请告知用户未找到匹配内容\n" +
                    "3. 回答不超过200字\n" +
                    "4. 不要使用Markdown格式\n" +
                    "5. 可以推荐相关的帖子或非遗项目\n";

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            Map<String, Object> contextMessage = new HashMap<>();
            contextMessage.put("role", "system");
            contextMessage.put("content", "参考资料：\n" + context.toString());
            messages.add(contextMessage);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userInput);
            messages.add(userMessage);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 300);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to generate knowledge answer", e);
        }

        return "根据搜索结果，为您找到了" + posts.size() + "篇相关帖子和" + heritages.size() + "个相关非遗项目，请查看下方详情。";
    }

    private Map<String, Object> extractQueryParams(String userInput) {
        String resolvedTimeContext = resolveRelativeTime(userInput);

        String systemPrompt = "你是一个查询参数提取助手。根据用户的自然语言，提取查询参数，以JSON格式返回。\n" +
                "活动表的可用字段：\n" +
                "- heritage_type: 非遗类型（如：刺绣、剪纸、陶艺、皮影、京剧等）\n" +
                "- location_city: 城市（如：北京、上海、广州等）\n" +
                "- price: 价格关键词（如：免费）\n" +
                "- price_max: 价格上限（数字，如50表示50元以下）\n" +
                "- start_date_after: 开始日期下限，格式 yyyy-MM-dd\n" +
                "- start_date_before: 开始日期上限，格式 yyyy-MM-dd\n" +
                "- start_date: 精确日期，格式 yyyy-MM-dd\n" +
                "\n" +
                "规则：\n" +
                "1. 只返回JSON，不要任何解释\n" +
                "2. 用户未提及的字段不要出现在JSON中\n" +
                "3. 日期必须是绝对日期，不要写相对时间\n" +
                "4. 今天是" + LocalDate.now().toString() + "\n" +
                "5. 用户输入中已解析的时间上下文：" + resolvedTimeContext + "\n" +
                "6. 当用户说'XX元以下'或'不超过XX元'时，使用price_max字段（数字），不要用price字段\n" +
                "\n" +
                "示例：\n" +
                "用户：下周六有什么刺绣类的非遗活动？\n" +
                "输出：{\"heritage_type\":\"刺绣\",\"start_date\":\"" + getNextSaturday().toString() + "\"}\n" +
                "\n" +
                "用户：北京有哪些免费的非遗活动？\n" +
                "输出：{\"location_city\":\"北京\",\"price\":\"免费\"}\n" +
                "\n" +
                "用户：有没有50元以下的陶艺活动？\n" +
                "输出：{\"heritage_type\":\"陶艺\",\"price_max\":50}\n" +
                "\n" +
                "用户：本周有什么非遗活动？\n" +
                "输出：{\"start_date_after\":\"" + LocalDate.now().toString() + "\",\"start_date_before\":\"" + LocalDate.now().plusWeeks(1).toString() + "\"}";

        try {
            Map<String, Object> llmParams = callDeepSeekForJSON(systemPrompt, userInput);
            if (llmParams != null && !llmParams.isEmpty()) {
                return llmParams;
            }
        } catch (Exception e) {
            // LLM调用失败，使用规则降级
        }

        return extractQueryParamsRuleBased(userInput);
    }

    private Map<String, Object> callDeepSeekForJSON(String systemPrompt, String userInput) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();

            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userInput);
            messages.add(userMessage);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 300);
            requestBody.put("temperature", 0.1);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null) {
                        String content = (String) message.get("content");
                        return parseJSONFromResponse(content);
                    }
                }
            }
        } catch (Exception e) {
            // 降级到规则方式
        }
        return null;
    }

    private Map<String, Object> parseJSONFromResponse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        String jsonStr = content.trim();

        // 提取JSON部分（可能被markdown代码块包裹）
        Matcher jsonMatcher = Pattern.compile("\\{[\\s\\S]*\\}").matcher(jsonStr);
        if (jsonMatcher.find()) {
            jsonStr = jsonMatcher.group();
        }

        try {
            return objectMapper.readValue(jsonStr, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> extractQueryParamsRuleBased(String userInput) {
        Map<String, Object> params = new HashMap<>();

        // 提取非遗类型
        String[] heritageTypes = {"刺绣", "剪纸", "陶艺", "皮影", "京剧", "昆曲", "书法", "国画", "茶艺", "木雕", "泥塑", "扎染"};
        for (String type : heritageTypes) {
            if (userInput.contains(type)) {
                params.put("heritage_type", type);
                break;
            }
        }

        // 提取城市
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "苏州", "成都", "西安", "南京", "武汉", "重庆", "长沙"};
        for (String city : cities) {
            if (userInput.contains(city)) {
                params.put("location_city", city);
                break;
            }
        }

        // 提取价格
        if (userInput.contains("免费")) {
            params.put("price", "免费");
        }

        // 提取时间（Java层计算绝对日期）
        resolveTimeParamsRuleBased(userInput, params);

        return params;
    }

    private void resolveTimeParamsRuleBased(String userInput, Map<String, Object> params) {
        LocalDate today = LocalDate.now();

        if (userInput.contains("今天")) {
            params.put("start_date", today.toString());
        } else if (userInput.contains("明天")) {
            params.put("start_date", today.plusDays(1).toString());
        } else if (userInput.contains("后天")) {
            params.put("start_date", today.plusDays(2).toString());
        } else if (userInput.contains("下周六")) {
            params.put("start_date", getNextSaturday().toString());
        } else if (userInput.contains("下周日") || userInput.contains("下周天")) {
            params.put("start_date", getNextSunday().toString());
        } else if (userInput.contains("下周")) {
            params.put("start_date_after", today.plusWeeks(1).toString());
            params.put("start_date_before", today.plusWeeks(2).toString());
        } else if (userInput.contains("本周")) {
            params.put("start_date_after", today.toString());
            params.put("start_date_before", today.plusWeeks(1).toString());
        } else if (userInput.contains("周末")) {
            LocalDate nextSat = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            LocalDate nextSun = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            params.put("start_date_after", nextSat.toString());
            params.put("start_date_before", nextSun.plusDays(1).toString());
        }
    }

    private String resolveRelativeTime(String userInput) {
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();

        if (userInput.contains("今天")) {
            sb.append("今天=").append(today).append("; ");
        }
        if (userInput.contains("明天")) {
            sb.append("明天=").append(today.plusDays(1)).append("; ");
        }
        if (userInput.contains("后天")) {
            sb.append("后天=").append(today.plusDays(2)).append("; ");
        }
        if (userInput.contains("下周六")) {
            sb.append("下周六=").append(getNextSaturday()).append("; ");
        }
        if (userInput.contains("下周日") || userInput.contains("下周天")) {
            sb.append("下周日=").append(getNextSunday()).append("; ");
        }
        if (userInput.contains("下周")) {
            sb.append("下周范围=").append(today.plusWeeks(1)).append("至").append(today.plusWeeks(2)).append("; ");
        }
        if (userInput.contains("本周")) {
            sb.append("本周范围=").append(today).append("至").append(today.plusWeeks(1)).append("; ");
        }

        return sb.length() > 0 ? sb.toString() : "无特殊时间表达";
    }

    private LocalDate getNextSaturday() {
        LocalDate today = LocalDate.now();
        LocalDate nextSat = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        // 如果今天是周六，"下周六"应该是下下周六
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nextSat = nextSat.plusWeeks(1);
        }
        return nextSat;
    }

    private LocalDate getNextSunday() {
        LocalDate today = LocalDate.now();
        LocalDate nextSun = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nextSun = nextSun.plusWeeks(1);
        }
        return nextSun;
    }

    private String buildSafeSQL(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM activities WHERE status != 'ended'");

        if (params.containsKey("heritage_type")) {
            String value = escapeSQLString(params.get("heritage_type").toString());
            sql.append(" AND (heritage_type LIKE '%").append(value).append("%'");
            sql.append(" OR title LIKE '%").append(value).append("%'");
            sql.append(" OR subtitle LIKE '%").append(value).append("%')");
        }

        if (params.containsKey("location_city")) {
            String value = escapeSQLString(params.get("location_city").toString());
            sql.append(" AND location_city LIKE '%").append(value).append("%'");
        }

        if (params.containsKey("price")) {
            String priceValue = params.get("price").toString();
            if ("免费".equals(priceValue) || "0".equals(priceValue)) {
                sql.append(" AND price = 0");
            } else {
                String value = escapeSQLString(priceValue);
                sql.append(" AND price LIKE '%").append(value).append("%'");
            }
        }

        if (params.containsKey("price_max")) {
            try {
                double maxPrice = Double.parseDouble(params.get("price_max").toString());
                sql.append(" AND price <= ").append((int) maxPrice);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        if (params.containsKey("start_date")) {
            String value = escapeSQLString(params.get("start_date").toString());
            sql.append(" AND DATE(start_time) = '").append(value).append("'");
        }

        if (params.containsKey("start_date_after")) {
            String value = escapeSQLString(params.get("start_date_after").toString());
            sql.append(" AND DATE(start_time) >= '").append(value).append("'");
        }

        if (params.containsKey("start_date_before")) {
            String value = escapeSQLString(params.get("start_date_before").toString());
            sql.append(" AND DATE(start_time) < '").append(value).append("'");
        }

        sql.append(" ORDER BY start_time ASC");

        return sql.toString();
    }

    private String escapeSQLString(String input) {
        if (input == null) return "";
        return input.replace("'", "''").replace("\\", "\\\\").replace(";", "").replace("--", "");
    }

    private boolean validateSQL(String sql) {
        String lowerSQL = sql.toLowerCase().trim();

        if (!lowerSQL.startsWith("select")) {
            return false;
        }

        // 检查是否包含分号后的多语句
        if (lowerSQL.contains(";")) {
            String afterSemicolon = lowerSQL.substring(lowerSQL.indexOf(";") + 1).trim();
            if (!afterSemicolon.isEmpty()) {
                return false;
            }
        }

        // 检查危险关键词
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (lowerSQL.contains(keyword)) {
                return false;
            }
        }

        return true;
    }

    private List<Activity> executeSafeSQLQuery(String sql) {
        if (!validateSQL(sql)) {
            return activityRepository.findByStatusNotOrderByStartTimeAsc("ended");
        }

        try {
            Query query = entityManager.createNativeQuery(sql, Activity.class);
            query.setMaxResults(20);
            return query.getResultList();
        } catch (Exception e) {
            return activityRepository.findByStatusNotOrderByStartTimeAsc("ended");
        }
    }

    private String generateEmptyResultMessage(String userInput) {
        try {
            String systemPrompt = "你是一个友好的助手。用户查询了非遗活动但没有找到结果，请用简短的中文给出友好提示，" +
                    "建议用户尝试其他关键词或放宽条件。不超过50字。不要使用Markdown格式。";

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "我搜索了「" + userInput + "」但没有找到结果");
            messages.add(userMessage);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 100);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
        } catch (Exception e) {
            // 降级到静态提示
        }

        return "抱歉，目前没有找到符合您条件的活动，可以试试其他关键词或放宽条件哦～";
    }

    private String extractDestination(String userInput) {
        List<String> cities = Arrays.asList("北京", "上海", "广州", "深圳", "杭州", "苏州", "成都", "西安", "南京", "武汉");
        for (String city : cities) {
            if (userInput.contains(city)) {
                return city;
            }
        }
        return "全国";
    }

    private int extractDays(String userInput) {
        String normalized = userInput
                .replace("一", "1").replace("二", "2").replace("两", "2")
                .replace("三", "3").replace("四", "4").replace("五", "5")
                .replace("六", "6").replace("七", "7").replace("八", "8")
                .replace("九", "9").replace("十", "10");
        for (int i = 7; i >= 1; i--) {
            if (normalized.contains(String.valueOf(i)) && (normalized.contains("天") || normalized.contains("日"))) {
                return i;
            }
        }
        return 2;
    }

    private List<Activity> findActivitiesByDestination(String destination) {
        if ("全国".equals(destination)) {
            return activityRepository.findByStatusNotOrderByStartTimeAsc("ended");
        } else {
            return activityRepository.findByStatusNotOrderByStartTimeAsc("ended").stream()
                    .filter(activity -> activity.getLocationCity() != null && activity.getLocationCity().contains(destination))
                    .collect(Collectors.toList());
        }
    }

    private List<IntangibleCulturalHeritage> findHeritagesByDestination(String destination) {
        if ("全国".equals(destination)) {
            return intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc();
        } else {
            return intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc().stream()
                    .filter(heritage -> heritage.getRegion() != null && heritage.getRegion().contains(destination))
                    .collect(Collectors.toList());
        }
    }

    private List<DiscoverPost> findPostsByKeyword(String keyword) {
        return discoverPostRepository.findByStatusOrderByCreateTimeDesc(1).stream()
                .filter(post -> post.getTitle() != null && post.getTitle().contains("非遗"))
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<IntangibleCulturalHeritage> findHeritagesByKeyword(String keyword) {
        return intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc().stream()
                .filter(heritage -> heritage.getName() != null && heritage.getName().contains("非遗"))
                .limit(10)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildItinerary(int days, List<Activity> activities, List<IntangibleCulturalHeritage> heritages) {
        Map<String, Object> itinerary = new HashMap<>();
        List<Map<String, Object>> daysPlan = new ArrayList<>();

        List<Map<String, Object>> activityCards = buildActivityCards(activities);
        List<Map<String, Object>> heritageCards = buildHeritageCards(heritages);

        for (int i = 1; i <= days; i++) {
            Map<String, Object> dayPlan = new HashMap<>();
            dayPlan.put("day", i);
            dayPlan.put("activities", activityCards.stream().skip((i - 1) * 2).limit(2).collect(Collectors.toList()));
            dayPlan.put("heritages", heritageCards.stream().skip((i - 1) * 2).limit(2).collect(Collectors.toList()));
            daysPlan.add(dayPlan);
        }

        itinerary.put("days", daysPlan);
        itinerary.put("summary", "为您规划了" + days + "天的非遗之旅，包含" + activities.size() + "个活动和" + heritages.size() + "个非遗项目。");

        return itinerary;
    }

    private List<Map<String, Object>> buildActivityCards(List<Activity> activities) {
        return activities.stream().map(activity -> {
            Map<String, Object> card = new HashMap<>();
            card.put("id", activity.getId());
            card.put("title", activity.getTitle());
            card.put("subtitle", activity.getSubtitle());
            card.put("heritageType", activity.getHeritageType());
            card.put("location", activity.getLocationCity() + (activity.getLocationDetail() != null ? " " + activity.getLocationDetail() : ""));
            card.put("startTime", formatDateTime(activity.getStartTime()));
            card.put("price", activity.getPrice());
            card.put("status", activity.getStatus());
            return card;
        }).collect(Collectors.toList());
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(date);
    }

    private List<Map<String, Object>> buildHeritageCards(List<IntangibleCulturalHeritage> heritages) {
        return heritages.stream().map(heritage -> {
            Map<String, Object> card = new HashMap<>();
            card.put("id", heritage.getId());
            card.put("name", heritage.getName());
            card.put("category", heritage.getCategory());
            card.put("region", heritage.getRegion());
            card.put("introduction", heritage.getIntroduction());
            return card;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPostCards(List<DiscoverPost> posts) {
        return posts.stream().map(post -> {
            Map<String, Object> card = new HashMap<>();
            card.put("id", post.getId());
            card.put("title", post.getTitle());
            card.put("content", post.getContent());
            card.put("userId", post.getUserId());
            card.put("createTime", post.getCreateTime());
            return card;
        }).collect(Collectors.toList());
    }

    public String buildSummaryText(Map<String, Object> result) {
        String intent = result.get("intent") != null ? result.get("intent").toString() : "";
        switch (intent) {
            case "ITINERARY_PLANNING":
                return "为您规划了" + result.get("days") + "天的" + result.get("destination") + "非遗之旅";
            case "STRUCTURED_QUERY":
                int total = result.get("total") != null ? Integer.parseInt(result.get("total").toString()) : 0;
                return total > 0 ? "为您找到 " + total + " 个相关活动" : "暂无符合条件的活动";
            case "KNOWLEDGE_QA":
                return "为您找到相关知识内容";
            default:
                return "查询完成";
        }
    }

    public String serializeResources(Map<String, Object> result) {
        try {
            Map<String, Object> toSave = new HashMap<>();
            toSave.put("intent", result.get("intent"));
            toSave.put("query", result.get("query"));
            if ("ITINERARY_PLANNING".equals(result.get("intent"))) {
                toSave.put("destination", result.get("destination"));
                toSave.put("days", result.get("days"));
                toSave.put("itinerary", result.get("itinerary"));
                toSave.put("activities", result.get("activities"));
                toSave.put("heritages", result.get("heritages"));
            } else if ("STRUCTURED_QUERY".equals(result.get("intent"))) {
                toSave.put("total", result.get("total"));
                toSave.put("activities", result.get("activities"));
                toSave.put("message", result.get("message"));
            } else if ("KNOWLEDGE_QA".equals(result.get("intent"))) {
                toSave.put("answer", result.get("answer"));
                toSave.put("posts", result.get("posts"));
                toSave.put("heritages", result.get("heritages"));
                toSave.put("totalPosts", result.get("totalPosts"));
                toSave.put("totalHeritages", result.get("totalHeritages"));
            }
            return objectMapper.writeValueAsString(toSave);
        } catch (Exception e) {
            logger.error("Failed to serialize resources", e);
            return "{}";
        }
    }

    public Map<String, Object> deserializeResources(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            logger.error("Failed to deserialize resources", e);
            return new HashMap<>();
        }
    }
}
