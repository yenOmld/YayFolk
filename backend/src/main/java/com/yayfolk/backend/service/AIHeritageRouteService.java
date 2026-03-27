package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.OfficialContent;
import com.yayfolk.backend.entity.Product;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.OfficialContentRepository;
import com.yayfolk.backend.repository.ProductRepository;
import com.yayfolk.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIHeritageRouteService {

    private static final List<String> KNOWN_CITIES = Arrays.asList(
            "北京", "上海", "广州", "深圳", "杭州", "苏州", "成都", "西安", "南京", "武汉",
            "长沙", "重庆", "泉州", "景德镇", "洛阳", "扬州", "福州"
    );

    private final ActivityRepository activityRepository;
    private final ProductRepository productRepository;
    private final OfficialContentRepository officialContentRepository;
    private final UserRepository userRepository;

    public AIHeritageRouteService(ActivityRepository activityRepository,
                                  ProductRepository productRepository,
                                  OfficialContentRepository officialContentRepository,
                                  UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.productRepository = productRepository;
        this.officialContentRepository = officialContentRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> generateRoute(Map<String, Object> request) {
        String userInput = request.get("userInput") == null ? "" : request.get("userInput").toString().trim();
        if (userInput.isEmpty()) {
            throw new RuntimeException("请输入路线需求");
        }

        String destination = detectDestination(userInput);
        int days = detectDays(userInput);
        List<String> preferences = detectPreferences(userInput);

        List<Activity> rankedActivities = rankActivities(userInput, destination, preferences);
        List<Product> rankedProducts = rankProducts(userInput, destination, preferences);
        List<OfficialContent> rankedContents = rankOfficialContents(userInput, destination, preferences);

        Map<String, Object> result = new HashMap<>();
        result.put("destination", destination);
        result.put("days", days);
        result.put("preferences", preferences);
        result.put("summary", buildSummary(destination, days, preferences, rankedActivities, rankedContents));
        result.put("itinerary", buildItinerary(days, rankedActivities, rankedContents));
        result.put("recommendedBusinesses", buildBusinesses(rankedActivities, rankedProducts));
        result.put("activities", buildActivityCards(rankedActivities));
        result.put("officialHighlights", buildOfficialHighlights(rankedContents));
        return result;
    }

    private String detectDestination(String userInput) {
        for (String city : KNOWN_CITIES) {
            if (userInput.contains(city)) {
                return city;
            }
        }

        for (Activity activity : activityRepository.findByStatusNotOrderByStartTimeAsc("ended")) {
            if (activity.getLocationCity() != null && userInput.contains(activity.getLocationCity())) {
                return activity.getLocationCity();
            }
        }

        return "全国";
    }

    private int detectDays(String userInput) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*天").matcher(userInput);
        if (matcher.find()) {
            int detected = Integer.parseInt(matcher.group(1));
            return Math.max(1, Math.min(detected, 7));
        }
        return 2;
    }

    private List<String> detectPreferences(String userInput) {
        LinkedHashSet<String> preferences = new LinkedHashSet<>();
        Map<String, String> keywordMap = new LinkedHashMap<>();
        keywordMap.put("工艺", "传统工艺");
        keywordMap.put("手作", "传统工艺");
        keywordMap.put("戏剧", "戏曲表演");
        keywordMap.put("京剧", "戏曲表演");
        keywordMap.put("昆曲", "戏曲表演");
        keywordMap.put("音乐", "传统音乐");
        keywordMap.put("古琴", "传统音乐");
        keywordMap.put("茶", "茶文化");
        keywordMap.put("美食", "地方风味");
        keywordMap.put("节气", "节气民俗");
        keywordMap.put("诗词", "诗意文化");
        keywordMap.put("亲子", "亲子体验");
        keywordMap.put("拍照", "文化打卡");

        for (Map.Entry<String, String> entry : keywordMap.entrySet()) {
            if (userInput.contains(entry.getKey())) {
                preferences.add(entry.getValue());
            }
        }

        if (preferences.isEmpty()) {
            preferences.add("传统工艺");
            preferences.add("文化体验");
        }

        return new ArrayList<>(preferences);
    }

    private List<Activity> rankActivities(String userInput, String destination, List<String> preferences) {
        List<Activity> activities = activityRepository.findByStatusNotOrderByStartTimeAsc("ended");
        activities.sort((left, right) -> Integer.compare(
                scoreActivity(right, userInput, destination, preferences),
                scoreActivity(left, userInput, destination, preferences)
        ));
        return activities.subList(0, Math.min(activities.size(), 6));
    }

    private int scoreActivity(Activity activity, String userInput, String destination, List<String> preferences) {
        int score = 0;
        if (!"全国".equals(destination) && destination.equals(activity.getLocationCity())) {
            score += 6;
        }
        score += matchScore(userInput, activity.getTitle(), 3);
        score += matchScore(userInput, activity.getHeritageType(), 4);
        score += matchScore(userInput, activity.getContent(), 2);
        score += matchPreference(preferences, activity.getTitle(), activity.getHeritageType(), activity.getContent());
        if ("signup".equals(activity.getStatus())) {
            score += 2;
        }
        return score;
    }

    private List<Product> rankProducts(String userInput, String destination, List<String> preferences) {
        List<Product> products = productRepository.findByStatusOrderByCreateTimeDesc("on_sale");
        products.sort((left, right) -> Integer.compare(
                scoreProduct(right, userInput, destination, preferences),
                scoreProduct(left, userInput, destination, preferences)
        ));
        return products.subList(0, Math.min(products.size(), 4));
    }

    private int scoreProduct(Product product, String userInput, String destination, List<String> preferences) {
        int score = 0;
        score += matchScore(userInput, product.getName(), 3);
        score += matchScore(userInput, product.getHeritageType(), 4);
        score += matchScore(userInput, product.getDescription(), 2);
        score += matchPreference(preferences, product.getName(), product.getHeritageType(), product.getDescription());
        if (product.getStock() != null && product.getStock() > 0) {
            score += 1;
        }
        if (product.getSales() != null) {
            score += Math.min(product.getSales() / 10, 3);
        }
        return score;
    }

    private List<OfficialContent> rankOfficialContents(String userInput, String destination, List<String> preferences) {
        List<OfficialContent> contents = officialContentRepository.findByIsPublicOrderByCreateTimeDesc(1);
        contents.sort((left, right) -> Integer.compare(
                scoreContent(right, userInput, destination, preferences),
                scoreContent(left, userInput, destination, preferences)
        ));
        return contents.subList(0, Math.min(contents.size(), 4));
    }

    private int scoreContent(OfficialContent content, String userInput, String destination, List<String> preferences) {
        int score = 0;
        score += matchScore(userInput, content.getTitle(), 4);
        score += matchScore(userInput, content.getContent(), 2);
        score += matchPreference(preferences, content.getTitle(), content.getCategory(), content.getContent());
        return score;
    }

    private int matchScore(String source, String target, int weight) {
        if (source == null || target == null) {
            return 0;
        }

        int score = 0;
        for (String token : tokenize(source)) {
            if (!token.isEmpty() && target.contains(token)) {
                score += weight;
            }
        }
        return score;
    }

    private int matchPreference(List<String> preferences, String... texts) {
        int score = 0;
        for (String preference : preferences) {
            for (String text : texts) {
                if (text != null && text.contains(preference)) {
                    score += 2;
                    break;
                }
            }
        }
        return score;
    }

    private List<String> tokenize(String text) {
        String normalized = text.replace("，", " ").replace("。", " ")
                .replace(",", " ").replace(".", " ").trim();
        List<String> tokens = new ArrayList<>();
        for (String part : normalized.split("\\s+")) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private String buildSummary(String destination, int days, List<String> preferences,
                                List<Activity> activities, List<OfficialContent> contents) {
        StringBuilder summary = new StringBuilder();
        summary.append("已为你生成一条");
        if (!"全国".equals(destination)) {
            summary.append(destination);
        }
        summary.append(days).append("天的非遗体验路线，重点围绕");
        summary.append(String.join("、", preferences));
        summary.append("展开。");

        if (!activities.isEmpty()) {
            summary.append("优先安排可预约的体验活动与在地商家。");
        }
        if (!contents.isEmpty()) {
            summary.append("同时补充官方非遗背景内容，方便边走边看。");
        }
        return summary.toString();
    }

    private List<Map<String, Object>> buildItinerary(int days, List<Activity> activities, List<OfficialContent> contents) {
        List<Map<String, Object>> itinerary = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", i + 1);
            day.put("theme", i == 0 ? "城市导入与非遗认知" : "深度体验与打卡");

            List<Map<String, Object>> dayActivities = new ArrayList<>();
            Activity activity = i < activities.size() ? activities.get(i) : null;
            OfficialContent content = i < contents.size() ? contents.get(i) : null;

            if (content != null) {
                Map<String, Object> intro = new HashMap<>();
                intro.put("time", "09:30");
                intro.put("name", content.getTitle());
                intro.put("description", abbreviate(content.getContent(), 72));
                intro.put("tags", Arrays.asList("官方导读", content.getCategory()));
                intro.put("details", Collections.singletonList(detail("内容类型", categoryLabel(content.getCategory()))));
                dayActivities.add(intro);
            }

            if (activity != null) {
                Map<String, Object> experience = new HashMap<>();
                experience.put("time", content == null ? "10:00" : "14:00");
                experience.put("name", activity.getTitle());
                experience.put("description", abbreviate(activity.getContent(), 88));
                experience.put("tags", buildActivityTags(activity));
                experience.put("details", Arrays.asList(
                        detail("城市", valueOrDefault(activity.getLocationCity(), "待定")),
                        detail("费用", activity.getPrice() == null || activity.getPrice() == 0 ? "免费" : "¥" + (activity.getPrice() / 100)),
                        detail("时间", formatDateTime(activity.getStartTime()))
                ));
                experience.put("business", buildBusiness(activity.getMerchantId(), activity.getLocationDetail()));
                dayActivities.add(experience);
            }

            if (dayActivities.isEmpty()) {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("time", "全天");
                fallback.put("name", "自由探索本地非遗空间");
                fallback.put("description", "建议结合官方内容先建立背景认知，再根据城市路线补充手作、展演或市集体验。");
                fallback.put("tags", Arrays.asList("自由行", "文化打卡"));
                fallback.put("details", Collections.singletonList(detail("建议", "关注近期活动上新")));
                dayActivities.add(fallback);
            }

            day.put("activities", dayActivities);
            itinerary.add(day);
        }
        return itinerary;
    }

    private List<Map<String, Object>> buildBusinesses(List<Activity> activities, List<Product> products) {
        LinkedHashMap<Long, Map<String, Object>> businesses = new LinkedHashMap<>();

        for (Activity activity : activities) {
            businesses.putIfAbsent(activity.getMerchantId(), businessCard(activity.getMerchantId(), activity.getLocationDetail(), activity.getHeritageType()));
        }

        for (Product product : products) {
            businesses.putIfAbsent(product.getMerchantId(), businessCard(product.getMerchantId(), null, product.getHeritageType()));
        }

        return new ArrayList<>(businesses.values()).subList(0, Math.min(businesses.size(), 4));
    }

    private List<Map<String, Object>> buildActivityCards(List<Activity> activities) {
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Activity activity : activities.subList(0, Math.min(activities.size(), 4))) {
            Map<String, Object> card = new HashMap<>();
            card.put("id", activity.getId());
            card.put("title", activity.getTitle());
            card.put("date", formatDateTime(activity.getStartTime()));
            card.put("description", abbreviate(activity.getContent(), 72));
            card.put("location", joinLocation(activity.getLocationCity(), activity.getLocationDetail()));
            cards.add(card);
        }
        return cards;
    }

    private List<Map<String, Object>> buildOfficialHighlights(List<OfficialContent> contents) {
        List<Map<String, Object>> highlights = new ArrayList<>();
        for (OfficialContent content : contents.subList(0, Math.min(contents.size(), 3))) {
            Map<String, Object> highlight = new HashMap<>();
            highlight.put("title", content.getTitle());
            highlight.put("category", categoryLabel(content.getCategory()));
            highlight.put("summary", abbreviate(content.getContent(), 80));
            highlights.add(highlight);
        }
        return highlights;
    }

    private Map<String, Object> detail(String label, String value) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("label", label);
        detail.put("value", value);
        return detail;
    }

    private Map<String, Object> buildBusiness(Long merchantId, String address) {
        Map<String, Object> business = new HashMap<>();
        Optional<User> merchant = userRepository.findById(merchantId);
        business.put("name", merchant.map(u -> u.getShopName() != null ? u.getShopName() : u.getNickname()).orElse("合作商家"));
        business.put("address", valueOrDefault(address, merchant.map(User::getLocation).orElse("以预约页面为准")));
        business.put("phone", merchant.map(User::getPhone).orElse("站内联系"));
        return business;
    }

    private Map<String, Object> businessCard(Long merchantId, String address, String heritageType) {
        Map<String, Object> card = new HashMap<>();
        Optional<User> merchant = userRepository.findById(merchantId);
        User user = merchant.orElse(null);
        card.put("name", user == null ? "合作商家" : valueOrDefault(user.getShopName(), user.getNickname()));
        card.put("address", valueOrDefault(address, user == null ? "以活动页为准" : valueOrDefault(user.getLocation(), "以活动页为准")));
        card.put("description", user == null ? "提供非遗体验与在地文化服务" : valueOrDefault(user.getShopIntro(), "提供非遗体验与在地文化服务"));
        card.put("tags", Arrays.asList(valueOrDefault(heritageType, "非遗体验"), "推荐商家"));
        return card;
    }

    private List<String> buildActivityTags(Activity activity) {
        List<String> tags = new ArrayList<>();
        tags.add(valueOrDefault(activity.getHeritageType(), "非遗体验"));
        tags.add("signup".equals(activity.getStatus()) ? "可预约" : valueOrDefault(activity.getStatus(), "活动"));
        if (activity.getActivityType() != null) {
            tags.add(activity.getActivityType());
        }
        return tags;
    }

    private String categoryLabel(String category) {
        if ("news".equals(category)) {
            return "文化资讯";
        }
        if ("announcement".equals(category)) {
            return "平台公告";
        }
        return "非遗介绍";
    }

    private String joinLocation(String city, String detail) {
        if (city == null && detail == null) {
            return "地点待定";
        }
        if (city == null) {
            return detail;
        }
        if (detail == null || detail.isEmpty()) {
            return city;
        }
        return city + " · " + detail;
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "待定";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "暂无详细介绍";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
