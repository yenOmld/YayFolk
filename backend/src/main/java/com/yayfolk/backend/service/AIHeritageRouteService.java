package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.IntangibleCulturalHeritage;
import com.yayfolk.backend.entity.OfficialContent;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.IntangibleCulturalHeritageRepository;
import com.yayfolk.backend.repository.OfficialContentRepository;
import com.yayfolk.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AIHeritageRouteService {

    private static final List<String> COMMON_PROVINCES = Arrays.asList(
            "北京", "天津", "上海", "重庆", "河北", "山西", "辽宁", "吉林", "黑龙江",
            "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南",
            "广东", "海南", "四川", "贵州", "云南", "陕西", "甘肃", "青海", "台湾",
            "内蒙古", "广西", "西藏", "宁夏", "新疆", "香港", "澳门"
    );

    private static final List<String> COMMON_CITIES = Arrays.asList(
            "北京", "上海", "广州", "深圳", "杭州", "苏州", "成都", "西安", "南京", "武汉",
            "长沙", "重庆", "泉州", "景德镇", "洛阳", "扬州", "福州", "天津", "黄山", "潍坊",
            "黔东南", "南宁", "丽水"
    );

    private static final Map<String, List<String>> PREFERENCE_GROUPS = buildPreferenceGroups();

    private static final Map<String, List<String>> DYNASTY_GROUPS = buildDynastyGroups();

    private static final Map<String, List<String>> CATEGORY_GROUPS = buildCategoryGroups();

    private static final Map<String, List<String>> THEME_GROUPS = buildThemeGroups();

    private static final Map<String, List<String>> REGION_GROUPS = buildRegionGroups();

    private static final List<String> QUERY_STOP_WORDS = Arrays.asList(
            "我想", "想去", "想在", "帮我", "请帮我", "安排", "推荐", "规划", "路线", "行程", "体验",
            "看看", "打算", "希望", "优先", "重点", "适合", "可以", "参加", "周末", "线下", "活动",
            "非遗", "文化", "一个", "一些", "这次", "旅行", "旅游", "出行", "地方",
            "的", "和", "与", "及", "在", "去", "玩"
    );

    private static final List<String> EXCLUSION_PREFIXES = Arrays.asList(
            "不喜欢", "不要", "排除", "避开", "不想要", "不想看", "不考虑"
    );

    private final ActivityRepository activityRepository;
    private final IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository;
    private final OfficialContentRepository officialContentRepository;
    private final UserRepository userRepository;

    public AIHeritageRouteService(ActivityRepository activityRepository,
                                  IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository,
                                  OfficialContentRepository officialContentRepository,
                                  UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.intangibleCulturalHeritageRepository = intangibleCulturalHeritageRepository;
        this.officialContentRepository = officialContentRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> generateRoute(Map<String, Object> request) {
        String userInput = request.get("userInput") == null ? "" : request.get("userInput").toString().trim();
        if (userInput.isEmpty()) {
            throw new RuntimeException("请输入路线需求");
        }

        List<Activity> candidateActivities = findCandidateActivities();
        List<IntangibleCulturalHeritage> candidateHeritages = findCandidateHeritages();
        DestinationScope destination = detectDestination(removeExclusionSegments(userInput), candidateActivities, candidateHeritages);
        int days = detectDays(userInput);
        int requestedProjectCount = detectRequestedProjectCount(userInput, days);
        List<String> dynasties = detectDynasties(userInput);
        List<String> themes = detectThemes(userInput);
        List<String> categories = detectCategories(userInput);
        List<String> regions = detectRegions(userInput, candidateActivities, candidateHeritages, themes);
        List<String> exclusions = detectExclusions(userInput);
        List<String> preferences = detectPreferences(userInput);
        List<String> intentKeywords = extractIntentKeywords(removeExclusionSegments(userInput), destination);
        List<String> queryKeywords = extractQueryKeywords(intentKeywords, preferences);

        List<IntangibleCulturalHeritage> rankedHeritages = rankHeritages(
                candidateHeritages,
                destination,
                dynasties,
                categories,
                themes,
                queryKeywords,
                preferences,
                requestedProjectCount,
                !intentKeywords.isEmpty(),
                exclusions
        );
        List<Activity> rankedActivities = diversifyActivities(rankActivities(
                candidateActivities,
                destination,
                queryKeywords,
                preferences,
                requestedProjectCount,
                !intentKeywords.isEmpty(),
                exclusions,
                rankedHeritages
        ), requestedProjectCount);
        List<IntangibleCulturalHeritage> relatedHeritages = selectRelatedHeritages(
                rankedActivities,
                rankedHeritages,
                candidateHeritages,
                Math.max(3, Math.min(requestedProjectCount, 6))
        );
        List<OfficialContent> rankedContents = rankOfficialContents(removeExclusionSegments(userInput), destination, preferences);

        Map<String, Object> result = new HashMap<>();
        result.put("destination", destination.getDisplayName());
        result.put("days", days);
        result.put("requestedProjectCount", requestedProjectCount);
        result.put("preferences", preferences);
        result.put("parsedRequest", buildParsedRequest(dynasties, regions, categories, themes, exclusions, days, requestedProjectCount));
        result.put("summary", buildSummary(destination, days, requestedProjectCount, preferences, rankedActivities, rankedContents));
        result.put("itinerary", buildExperienceItinerary(days, rankedActivities));
        result.put("recommendedBusinesses", buildBusinesses(rankedActivities));
        result.put("activities", buildActivityCards(rankedActivities));
        result.put("officialHighlights", buildRelatedHeritageHighlights(rankedActivities, relatedHeritages, rankedContents));
        result.put("matchedCount", relatedHeritages.size() + rankedActivities.size());
        return result;
    }

    private List<Activity> findCandidateActivities() {
        List<Activity> approved = activityRepository.findByAuditStatusAndStatusNotOrderByStartTimeAsc("approved", "ended");
        if (!approved.isEmpty()) {
            return approved;
        }
        return activityRepository.findByStatusNotOrderByStartTimeAsc("ended");
    }

    private List<IntangibleCulturalHeritage> findCandidateHeritages() {
        return intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc();
    }

    private DestinationScope detectDestination(String userInput, List<Activity> activities, List<IntangibleCulturalHeritage> heritages) {
        List<String> provinceCandidates = new ArrayList<>(COMMON_PROVINCES);
        List<String> cityCandidates = new ArrayList<>(COMMON_CITIES);

        for (Activity activity : activities) {
            if (hasText(activity.getLocationProvince())) {
                provinceCandidates.add(activity.getLocationProvince().trim());
            }
            if (hasText(activity.getLocationCity())) {
                cityCandidates.add(activity.getLocationCity().trim());
            }
        }
        for (IntangibleCulturalHeritage heritage : heritages) {
            provinceCandidates.addAll(extractRegionFragments(heritage.getRegion(), true));
            cityCandidates.addAll(extractRegionFragments(heritage.getRegion(), false));
        }

        Optional<String> city = cityCandidates.stream()
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .filter(name -> userInput.contains(name))
                .findFirst();
        if (city.isPresent()) {
            return DestinationScope.city(city.get());
        }

        Optional<String> province = provinceCandidates.stream()
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .filter(name -> userInput.contains(name))
                .findFirst();
        if (province.isPresent()) {
            return DestinationScope.province(province.get());
        }

        return DestinationScope.nationwide();
    }

    private int detectDays(String userInput) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*[天日]").matcher(userInput);
        if (matcher.find()) {
            int detected = Integer.parseInt(matcher.group(1));
            return Math.max(1, Math.min(detected, 7));
        }
        return 2;
    }

    private int detectRequestedProjectCount(String userInput, int days) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*(个|项|场)(项目|活动|体验|行程)?").matcher(userInput);
        if (matcher.find()) {
            int detected = Integer.parseInt(matcher.group(1));
            return Math.max(1, Math.min(detected, 8));
        }
        return Math.max(days * 2, 4);
    }

    private List<String> detectPreferences(String userInput) {
        String positiveInput = removeExclusionSegments(userInput);
        LinkedHashSet<String> preferences = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : PREFERENCE_GROUPS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (positiveInput.contains(keyword)) {
                    preferences.add(entry.getKey());
                    break;
                }
            }
        }

        if (preferences.isEmpty()) {
            preferences.add("手作体验");
            preferences.add("文化漫游");
        }

        return new ArrayList<>(preferences);
    }

    private Map<String, Object> buildParsedRequest(List<String> dynasties,
                                                   List<String> regions,
                                                   List<String> categories,
                                                   List<String> themes,
                                                   List<String> exclusions,
                                                   int days,
                                                   int requestedProjectCount) {
        Map<String, Object> parsedRequest = new LinkedHashMap<>();
        parsedRequest.put("dynasties", dynasties);
        parsedRequest.put("regions", regions);
        parsedRequest.put("categories", categories);
        parsedRequest.put("themes", themes);
        parsedRequest.put("exclusions", exclusions);
        parsedRequest.put("days", days);
        parsedRequest.put("requestedProjectCount", requestedProjectCount);
        return parsedRequest;
    }

    private List<String> detectDynasties(String userInput) {
        return detectOrderedLabels(userInput, DYNASTY_GROUPS);
    }

    private List<String> detectThemes(String userInput) {
        return detectOrderedLabels(userInput, THEME_GROUPS);
    }

    private List<String> detectCategories(String userInput) {
        String positiveInput = removeExclusionSegments(userInput);
        LinkedHashSet<String> categories = new LinkedHashSet<>();

        if (positiveInput.contains("传统技艺")) {
            categories.add("传统手工艺");
            categories.add("传统技艺");
        }

        if (positiveInput.contains("少数民族")) {
            categories.add("传统手工艺");
            categories.add("少数民族非遗");
        }

        categories.addAll(detectOrderedLabels(positiveInput, CATEGORY_GROUPS));
        return new ArrayList<>(categories);
    }

    private List<String> detectRegions(String userInput,
                                       List<Activity> activities,
                                       List<IntangibleCulturalHeritage> heritages,
                                       List<String> themes) {
        String positiveInput = removeExclusionSegments(userInput);
        LinkedHashSet<String> regions = new LinkedHashSet<>();

        List<MatchedLabel> broadRegions = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : REGION_GROUPS.entrySet()) {
            int index = positiveInput.indexOf(entry.getKey());
            if (index >= 0) {
                broadRegions.add(new MatchedLabel(entry.getKey(), index));
            }
        }
        broadRegions.sort(Comparator.comparingInt(MatchedLabel::getIndex));
        for (MatchedLabel broadRegion : broadRegions) {
            regions.addAll(REGION_GROUPS.getOrDefault(broadRegion.getLabel(), Collections.emptyList()));
        }

        List<String> provinceCandidates = new ArrayList<>(COMMON_PROVINCES);
        List<String> cityCandidates = new ArrayList<>(COMMON_CITIES);
        for (Activity activity : activities) {
            if (hasText(activity.getLocationProvince())) {
                provinceCandidates.add(activity.getLocationProvince().trim());
            }
            if (hasText(activity.getLocationCity())) {
                cityCandidates.add(activity.getLocationCity().trim());
            }
        }
        for (IntangibleCulturalHeritage heritage : heritages) {
            provinceCandidates.addAll(extractRegionFragments(heritage.getRegion(), true));
            cityCandidates.addAll(extractRegionFragments(heritage.getRegion(), false));
        }

        List<MatchedLabel> directRegions = new ArrayList<>();
        for (String region : mergeRegionCandidates(provinceCandidates, cityCandidates)) {
            int index = positiveInput.indexOf(region);
            if (index >= 0) {
                directRegions.add(new MatchedLabel(region, index));
            }
        }
        directRegions.sort(Comparator.comparingInt(MatchedLabel::getIndex)
                .thenComparing((MatchedLabel item) -> item.getLabel().length(), Comparator.reverseOrder()));
        for (MatchedLabel directRegion : directRegions) {
            regions.add(directRegion.getLabel());
        }

        if (regions.isEmpty() && (containsAny(userInput, "全国", "全国范围", "全国范围内", "全国都可以") || !themes.isEmpty())) {
            regions.add("全国");
        }

        return new ArrayList<>(regions);
    }

    private List<String> mergeRegionCandidates(List<String> provinces, List<String> cities) {
        LinkedHashSet<String> regions = new LinkedHashSet<>();
        regions.addAll(provinces);
        regions.addAll(cities);
        return regions.stream()
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());
    }

    private List<String> extractRegionFragments(String region, boolean provinceOnly) {
        if (!hasText(region)) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> fragments = new LinkedHashSet<>();
        String[] segments = region.split("[、,，/\\s]+");
        for (String rawSegment : segments) {
            String segment = rawSegment == null ? "" : rawSegment.trim();
            if (!hasText(segment)) {
                continue;
            }

            Matcher provinceMatcher = Pattern.compile("[^,，、]*?(省|市|自治区|壮族自治区|回族自治区|维吾尔自治区|特别行政区)").matcher(segment);
            if (provinceMatcher.find()) {
                fragments.add(provinceMatcher.group());
            }

            if (!provinceOnly) {
                Matcher cityMatcher = Pattern.compile("[^,，、]*?(市|州|地区|盟|县)").matcher(segment);
                while (cityMatcher.find()) {
                    fragments.add(cityMatcher.group());
                }
            }

            if (!provinceOnly && !segment.endsWith("省") && !segment.endsWith("自治区") && !segment.endsWith("特别行政区")) {
                fragments.add(segment);
            }
        }

        return new ArrayList<>(fragments);
    }

    private List<String> detectExclusions(String userInput) {
        LinkedHashSet<String> exclusions = new LinkedHashSet<>();
        for (String prefix : EXCLUSION_PREFIXES) {
            Matcher matcher = Pattern.compile(Pattern.quote(prefix) + "([^，。；;！？!?]+)").matcher(userInput);
            while (matcher.find()) {
                String segment = matcher.group(1);
                for (String item : splitExclusionItems(segment)) {
                    String normalized = normalizeExclusion(item);
                    if (hasText(normalized)) {
                        exclusions.add(normalized);
                    }
                }
            }
        }
        return new ArrayList<>(exclusions);
    }

    private List<String> splitExclusionItems(String segment) {
        if (!hasText(segment)) {
            return Collections.emptyList();
        }

        List<String> items = new ArrayList<>();
        for (String item : segment.split("(、|，|,|/|／|和|与|及|以及)")) {
            if (hasText(item)) {
                items.add(item.trim());
            }
        }
        if (items.isEmpty()) {
            items.add(segment.trim());
        }
        return items;
    }

    private String removeExclusionSegments(String userInput) {
        String sanitized = userInput;
        for (String prefix : EXCLUSION_PREFIXES) {
            sanitized = sanitized.replaceAll(Pattern.quote(prefix) + "[^，。；;！？!?]+", " ");
        }
        return sanitized;
    }

    private String normalizeExclusion(String item) {
        if (!hasText(item)) {
            return "";
        }

        String normalized = item.trim()
                .replace("的", "")
                .replace("类", "类")
                .replaceAll("\\s+", "");

        if (normalized.contains("戏曲")) {
            return "传统戏曲";
        }
        if (normalized.contains("手工艺") || normalized.contains("技艺") || normalized.contains("工艺")) {
            return "传统手工艺";
        }
        if (normalized.contains("美术")) {
            return "传统美术";
        }
        if (normalized.contains("民俗")) {
            return "传统民俗";
        }

        return normalized;
    }

    private List<String> detectOrderedLabels(String userInput, Map<String, List<String>> groups) {
        List<MatchedLabel> matchedLabels = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            int index = firstIndexOf(userInput, entry.getValue());
            if (index >= 0) {
                matchedLabels.add(new MatchedLabel(entry.getKey(), index));
            }
        }

        matchedLabels.sort(Comparator.comparingInt(MatchedLabel::getIndex));
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (MatchedLabel matchedLabel : matchedLabels) {
            labels.add(matchedLabel.getLabel());
        }
        return new ArrayList<>(labels);
    }

    private int firstIndexOf(String source, List<String> keywords) {
        int earliest = Integer.MAX_VALUE;
        for (String keyword : keywords) {
            int index = source.indexOf(keyword);
            if (index >= 0 && index < earliest) {
                earliest = index;
            }
        }
        return earliest == Integer.MAX_VALUE ? -1 : earliest;
    }

    private List<IntangibleCulturalHeritage> rankHeritages(List<IntangibleCulturalHeritage> heritages,
                                                           DestinationScope destination,
                                                           List<String> dynasties,
                                                           List<String> categories,
                                                           List<String> themes,
                                                           List<String> queryKeywords,
                                                           List<String> preferences,
                                                           int requestedProjectCount,
                                                           boolean hasSpecificIntent,
                                                           List<String> exclusions) {
        if (heritages.isEmpty()) {
            return Collections.emptyList();
        }

        List<IntangibleCulturalHeritage> scopedHeritages = heritages;
        if (!destination.isNationwide()) {
            scopedHeritages = heritages.stream()
                    .filter(destination::matches)
                    .collect(Collectors.toList());
            if (scopedHeritages.isEmpty()) {
                return Collections.emptyList();
            }
        }

        scopedHeritages = scopedHeritages.stream()
                .filter(heritage -> !matchesExcludedHeritage(heritage, exclusions))
                .collect(Collectors.toList());
        if (scopedHeritages.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredHeritage> scored = new ArrayList<>();
        for (IntangibleCulturalHeritage heritage : scopedHeritages) {
            int score = scoreHeritage(heritage, destination, dynasties, categories, themes, queryKeywords, preferences);
            if (score > 0) {
                scored.add(new ScoredHeritage(heritage, score));
            }
        }

        if (scored.isEmpty()) {
            if (!destination.isNationwide() || hasSpecificIntent) {
                return Collections.emptyList();
            }
            return scopedHeritages.stream()
                    .sorted(Comparator
                            .comparing((IntangibleCulturalHeritage heritage) -> safeInt(heritage.getIsFeatured())).reversed()
                            .thenComparing(heritage -> safeInt(heritage.getViewCount()), Comparator.reverseOrder())
                            .thenComparing(IntangibleCulturalHeritage::getId))
                    .limit(Math.min(requestedProjectCount, 8))
                    .collect(Collectors.toList());
        }

        scored.sort(Comparator.comparingInt(ScoredHeritage::getScore).reversed()
                .thenComparing(item -> safeInt(item.getHeritage().getIsFeatured()), Comparator.reverseOrder())
                .thenComparing(item -> safeInt(item.getHeritage().getViewCount()), Comparator.reverseOrder())
                .thenComparing(item -> item.getHeritage().getId()));

        return scored.stream()
                .map(ScoredHeritage::getHeritage)
                .limit(Math.min(requestedProjectCount, 8))
                .collect(Collectors.toList());
    }

    private int scoreHeritage(IntangibleCulturalHeritage heritage,
                              DestinationScope destination,
                              List<String> dynasties,
                              List<String> categories,
                              List<String> themes,
                              List<String> queryKeywords,
                              List<String> preferences) {
        int score = 0;

        if (destination.isCity() && destination.matches(heritage)) {
            score += 28;
        } else if (destination.isProvince() && destination.matches(heritage)) {
            score += 20;
        }

        if (safeInt(heritage.getIsFeatured()) == 1) {
            score += 4;
        }

        score += safeInt(heritage.getViewCount()) / 100;
        score += matchKeywords(queryKeywords, heritage.getName(), 12);
        score += matchKeywords(queryKeywords, heritage.getCategory(), 8);
        score += matchKeywords(queryKeywords, heritage.getSubcategory(), 8);
        score += matchKeywords(queryKeywords, heritage.getDynasty(), 7);
        score += matchKeywords(queryKeywords, heritage.getRegion(), 7);
        score += matchKeywords(queryKeywords, heritage.getIntroduction(), 4);
        score += matchKeywords(queryKeywords, heritage.getHistory(), 3);
        score += matchKeywords(queryKeywords, heritage.getInheritanceValue(), 3);
        score += matchKeywords(queryKeywords, heritage.getRelatedSolarTerms(), 4);

        score += matchExactLabels(dynasties, heritage.getDynasty(), 10);
        score += matchExactLabels(categories, heritage.getCategory(), 10);
        score += matchExactLabels(categories, heritage.getSubcategory(), 6);
        score += matchThemeKeywords(themes, heritage);

        for (String preference : preferences) {
            List<String> keywords = expandPreferenceKeywords(preference);
            score += matchKeywords(keywords, heritage.getName(), 6);
            score += matchKeywords(keywords, heritage.getCategory(), 5);
            score += matchKeywords(keywords, heritage.getSubcategory(), 4);
            score += matchKeywords(keywords, heritage.getIntroduction(), 2);
        }

        return score;
    }

    private int matchExactLabels(List<String> labels, String text, int weight) {
        if (labels == null || labels.isEmpty() || !hasText(text)) {
            return 0;
        }

        int score = 0;
        for (String label : labels) {
            if (hasText(label) && text.contains(label)) {
                score += weight;
            }
        }
        return score;
    }

    private int matchThemeKeywords(List<String> themes, IntangibleCulturalHeritage heritage) {
        if (themes == null || themes.isEmpty()) {
            return 0;
        }

        int score = 0;
        for (String theme : themes) {
            List<String> keywords = THEME_GROUPS.getOrDefault(theme, Collections.singletonList(theme));
            score += matchKeywords(keywords, heritage.getName(), 5);
            score += matchKeywords(keywords, heritage.getIntroduction(), 3);
            score += matchKeywords(keywords, heritage.getRelatedSolarTerms(), 6);
        }
        return score;
    }

    private boolean matchesExcludedHeritage(IntangibleCulturalHeritage heritage, List<String> exclusions) {
        if (exclusions == null || exclusions.isEmpty()) {
            return false;
        }

        String searchableText = normalizeMatchText(String.join(" ",
                valueOrDefault(heritage.getName(), ""),
                valueOrDefault(heritage.getCategory(), ""),
                valueOrDefault(heritage.getSubcategory(), ""),
                valueOrDefault(heritage.getDynasty(), ""),
                valueOrDefault(heritage.getRegion(), ""),
                valueOrDefault(heritage.getIntroduction(), ""),
                valueOrDefault(heritage.getHistory(), ""),
                valueOrDefault(heritage.getInheritanceValue(), "")
        ));

        for (String exclusion : exclusions) {
            for (String keyword : expandExclusionKeywords(exclusion)) {
                String normalizedKeyword = normalizeMatchText(keyword);
                if (normalizedKeyword.length() >= 2 && searchableText.contains(normalizedKeyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Activity> rankActivities(List<Activity> activities,
                                          DestinationScope destination,
                                          List<String> queryKeywords,
                                          List<String> preferences,
                                          int requestedProjectCount,
                                          boolean hasSpecificIntent,
                                          List<String> exclusions,
                                          List<IntangibleCulturalHeritage> rankedHeritages) {
        if (requestedProjectCount <= 0 || activities.isEmpty()) {
            return Collections.emptyList();
        }

        List<Activity> scopedActivities = activities;
        if (!destination.isNationwide()) {
            scopedActivities = activities.stream()
                    .filter(destination::matches)
                    .collect(Collectors.toList());
            if (scopedActivities.isEmpty()) {
                return Collections.emptyList();
            }
        }
        scopedActivities = scopedActivities.stream()
                .filter(activity -> !matchesExcludedActivity(activity, exclusions))
                .collect(Collectors.toList());
        if (scopedActivities.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredActivity> scored = new ArrayList<>();
        List<String> heritageKeywords = extractHeritageKeywords(rankedHeritages);
        for (Activity activity : scopedActivities) {
            int score = scoreActivity(activity, destination, queryKeywords, preferences, heritageKeywords);
            if (score > 0) {
                scored.add(new ScoredActivity(activity, score));
            }
        }

        if (scored.isEmpty()) {
            if (!destination.isNationwide() || hasSpecificIntent) {
                return Collections.emptyList();
            }
            return scopedActivities.stream()
                    .sorted(Comparator
                            .comparing((Activity activity) -> safeInt(activity.getIsRecommend())).reversed()
                            .thenComparing(activity -> safeInt(activity.getCollectCount()), Comparator.reverseOrder())
                            .thenComparing(activity -> safeInt(activity.getSignupCount()), Comparator.reverseOrder())
                            .thenComparing(Activity::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(Math.min(requestedProjectCount, 8))
                    .collect(Collectors.toList());
        }

        scored.sort(Comparator.comparingInt(ScoredActivity::getScore).reversed()
                .thenComparing(item -> safeInt(item.getActivity().getIsRecommend()), Comparator.reverseOrder())
                .thenComparing(item -> safeInt(item.getActivity().getCollectCount()), Comparator.reverseOrder())
                .thenComparing(item -> safeInt(item.getActivity().getSignupCount()), Comparator.reverseOrder())
                .thenComparing(item -> item.getActivity().getStartTime(), Comparator.nullsLast(Comparator.naturalOrder())));

        return scored.stream()
                .map(ScoredActivity::getActivity)
                .limit(Math.min(requestedProjectCount, 8))
                .collect(Collectors.toList());
    }


    private List<Activity> diversifyActivities(List<Activity> activities, int requestedProjectCount) {
        if (activities.isEmpty()) {
            return activities;
        }

        int limit = Math.min(Math.max(requestedProjectCount, 1), Math.min(activities.size(), 8));
        LinkedHashMap<String, List<Activity>> grouped = new LinkedHashMap<>();
        for (Activity activity : activities) {
            grouped.computeIfAbsent(activityDiversityKey(activity), key -> new ArrayList<>()).add(activity);
        }

        List<Activity> diversified = new ArrayList<>();
        int round = 0;
        while (diversified.size() < limit) {
            boolean pickedAny = false;
            for (List<Activity> bucket : grouped.values()) {
                if (round < bucket.size()) {
                    diversified.add(bucket.get(round));
                    pickedAny = true;
                    if (diversified.size() >= limit) {
                        break;
                    }
                }
            }
            if (!pickedAny) {
                break;
            }
            round++;
        }
        return diversified;
    }

    private String activityDiversityKey(Activity activity) {
        if (hasText(activity.getHeritageType())) {
            return activity.getHeritageType().trim();
        }
        if (hasText(activity.getActivityType())) {
            return activity.getActivityType().trim();
        }
        return valueOrDefault(activity.getTitle(), "活动");
    }
    private int scoreActivity(Activity activity,
                              DestinationScope destination,
                              List<String> queryKeywords,
                              List<String> preferences,
                              List<String> heritageKeywords) {
        int score = 0;

        if (destination.isCity() && destination.matches(activity)) {
            score += 24;
        } else if (destination.isProvince() && destination.matches(activity)) {
            score += 18;
        }

        if ("signup".equals(activity.getStatus()) || "active".equals(activity.getStatus())) {
            score += 3;
        }

        if (safeInt(activity.getIsRecommend()) == 1) {
            score += 2;
        }

        score += matchKeywords(queryKeywords, activity.getTitle(), 10);
        score += matchKeywords(queryKeywords, activity.getSubtitle(), 6);
        score += matchKeywords(queryKeywords, activity.getActivityType(), 5);
        score += matchKeywords(queryKeywords, activity.getHeritageType(), 7);
        score += matchKeywords(queryKeywords, activity.getContent(), 3);
        score += matchKeywords(queryKeywords, activity.getLocationProvince(), 9);
        score += matchKeywords(queryKeywords, activity.getLocationCity(), 8);
        score += matchKeywords(queryKeywords, activity.getLocationDetail(), 4);
        score += matchKeywords(heritageKeywords, activity.getTitle(), 8);
        score += matchKeywords(heritageKeywords, activity.getHeritageType(), 8);
        score += matchKeywords(heritageKeywords, activity.getContent(), 4);

        score += matchPreferences(preferences, activity);
        return score;
    }

    private boolean matchesExcludedActivity(Activity activity, List<String> exclusions) {
        if (exclusions == null || exclusions.isEmpty()) {
            return false;
        }

        String searchableText = normalizeMatchText(String.join(" ",
                valueOrDefault(activity.getTitle(), ""),
                valueOrDefault(activity.getSubtitle(), ""),
                valueOrDefault(activity.getActivityType(), ""),
                valueOrDefault(activity.getHeritageType(), ""),
                valueOrDefault(activity.getContent(), ""),
                valueOrDefault(activity.getLocationProvince(), ""),
                valueOrDefault(activity.getLocationCity(), ""),
                valueOrDefault(activity.getLocationDetail(), "")
        ));

        for (String exclusion : exclusions) {
            for (String keyword : expandExclusionKeywords(exclusion)) {
                String normalizedKeyword = normalizeMatchText(keyword);
                if (normalizedKeyword.length() >= 2 && searchableText.contains(normalizedKeyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> expandCategoryKeywords(String value) {
        List<String> keywords = CATEGORY_GROUPS.get(value);
        if (keywords != null && !keywords.isEmpty()) {
            return keywords;
        }
        if ("传统技艺".equals(value)) {
            return Arrays.asList("传统技艺", "技艺", "手工艺", "工艺");
        }
        return Collections.singletonList(value);
    }

    private List<String> expandExclusionKeywords(String exclusion) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>(expandCategoryKeywords(exclusion));
        if (!hasText(exclusion)) {
            return new ArrayList<>(keywords);
        }

        String normalized = exclusion.trim()
                .replace("的", "")
                .replace("相关", "")
                .replace("有关", "")
                .replaceAll("\\s+", "");

        LinkedHashSet<String> regionCandidates = new LinkedHashSet<>();
        regionCandidates.addAll(COMMON_PROVINCES);
        regionCandidates.addAll(COMMON_CITIES);
        for (List<String> groupedRegions : REGION_GROUPS.values()) {
            regionCandidates.addAll(groupedRegions);
        }

        String remaining = normalized;
        for (String region : regionCandidates.stream()
                .filter(this::hasText)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList())) {
            if (remaining.contains(region)) {
                keywords.add(region);
                remaining = remaining.replace(region, " ");
            }
        }

        for (String token : remaining.split("[、,，/\\s]+")) {
            if (hasText(token) && token.trim().length() >= 2) {
                keywords.add(token.trim());
            }
        }

        return keywords.stream()
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<OfficialContent> rankOfficialContents(String userInput, DestinationScope destination, List<String> preferences) {
        List<OfficialContent> contents = officialContentRepository.findByIsPublicOrderByCreateTimeDesc(1);
        if (contents.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> queryKeywords = extractQueryKeywords(extractIntentKeywords(userInput, destination), preferences);
        contents.sort((left, right) -> Integer.compare(
                scoreContent(right, queryKeywords, preferences),
                scoreContent(left, queryKeywords, preferences)
        ));

        return contents.stream()
                .filter(content -> scoreContent(content, queryKeywords, preferences) > 0)
                .limit(4)
                .collect(Collectors.toList());
    }

    private int scoreContent(OfficialContent content, List<String> queryKeywords, List<String> preferences) {
        int score = 0;
        score += matchKeywords(queryKeywords, content.getTitle(), 8);
        score += matchKeywords(queryKeywords, content.getContent(), 3);
        for (String preference : preferences) {
            List<String> keywords = expandPreferenceKeywords(preference);
            score += matchKeywords(keywords, content.getTitle(), 4);
            score += matchKeywords(keywords, content.getContent(), 2);
        }
        return score;
    }

    private List<String> extractIntentKeywords(String userInput, DestinationScope destination) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();

        if (!destination.isNationwide()) {
            keywords.add(destination.getName());
        }

        String normalized = normalizeQuery(userInput);
        for (String part : normalized.split("\\s+")) {
            String token = part.trim();
            if (token.length() >= 2) {
                keywords.add(token);
                keywords.addAll(extractChineseNgrams(token));
                keywords.addAll(expandSemanticKeywords(token));
            }
        }

        return keywords.stream()
                .filter(keyword -> keyword != null && keyword.trim().length() >= 2)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());
    }

    private List<String> extractQueryKeywords(List<String> intentKeywords, List<String> preferences) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>(intentKeywords);

        for (String preference : preferences) {
            keywords.add(preference);
            keywords.addAll(expandPreferenceKeywords(preference));
        }

        return keywords.stream()
                .filter(keyword -> keyword != null && keyword.trim().length() >= 2)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());
    }

    private List<String> extractChineseNgrams(String token) {
        LinkedHashSet<String> ngrams = new LinkedHashSet<>();
        if (!token.matches(".*\\p{IsHan}.*")) {
            return new ArrayList<>(ngrams);
        }

        String compact = token.replaceAll("\\s+", "");
        int maxGram = Math.min(4, compact.length());
        for (int size = 2; size <= maxGram; size++) {
            for (int start = 0; start + size <= compact.length(); start++) {
                ngrams.add(compact.substring(start, start + size));
            }
        }
        return new ArrayList<>(ngrams);
    }

    private String normalizeQuery(String userInput) {
        String normalized = userInput;
        for (String stopWord : QUERY_STOP_WORDS) {
            normalized = normalized.replace(stopWord, " ");
        }
        normalized = normalized.replaceAll("[^\\p{IsHan}A-Za-z0-9]+", " ");
        return normalized.trim();
    }

    private List<String> expandSemanticKeywords(String token) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : PREFERENCE_GROUPS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (token.contains(keyword) || keyword.contains(token)) {
                    expanded.addAll(entry.getValue());
                    break;
                }
            }
        }
        return new ArrayList<>(expanded);
    }

    private List<String> expandPreferenceKeywords(String preference) {
        return PREFERENCE_GROUPS.getOrDefault(preference, Collections.singletonList(preference));
    }

    private int matchKeywords(List<String> keywords, String text, int weight) {
        if (keywords == null || keywords.isEmpty() || !hasText(text)) {
            return 0;
        }

        int score = 0;
        String normalizedText = normalizeMatchText(text);
        for (String keyword : keywords) {
            String normalizedKeyword = normalizeMatchText(keyword);
            if (normalizedKeyword.length() < 2) {
                continue;
            }
            if (normalizedText.contains(normalizedKeyword)) {
                score += weight + Math.min(normalizedKeyword.length(), 4);
            }
        }
        return score;
    }

    private int matchPreferences(List<String> preferences, Activity activity) {
        int score = 0;
        for (String preference : preferences) {
            List<String> keywords = expandPreferenceKeywords(preference);
            score += matchKeywords(keywords, activity.getTitle(), 6);
            score += matchKeywords(keywords, activity.getSubtitle(), 4);
            score += matchKeywords(keywords, activity.getContent(), 2);
            score += matchKeywords(keywords, activity.getHeritageType(), 4);
        }
        return score;
    }

    private List<String> extractHeritageKeywords(List<IntangibleCulturalHeritage> heritages) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (IntangibleCulturalHeritage heritage : heritages) {
            addKeywordTokens(keywords, heritage.getName());
            addKeywordTokens(keywords, heritage.getCategory());
            addKeywordTokens(keywords, heritage.getSubcategory());
            addKeywordTokens(keywords, heritage.getDynasty());
            addKeywordTokens(keywords, heritage.getRegion());
        }
        return keywords.stream()
                .filter(keyword -> keyword.length() >= 2)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());
    }

    private void addKeywordTokens(LinkedHashSet<String> keywords, String text) {
        if (!hasText(text)) {
            return;
        }
        String normalized = text.replaceAll("[^\\p{IsHan}A-Za-z0-9]+", " ").trim();
        if (normalized.isEmpty()) {
            return;
        }
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 2) {
                keywords.add(token);
                keywords.addAll(extractChineseNgrams(token));
            }
        }
    }

    private String normalizeMatchText(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String buildSummary(DestinationScope destination,
                                int days,
                                int requestedProjectCount,
                                List<String> preferences,
                                List<IntangibleCulturalHeritage> heritages,
                                List<Activity> activities,
                                List<OfficialContent> contents) {
        StringBuilder summary = new StringBuilder();
        if (heritages.isEmpty() && activities.isEmpty()) {
            summary.append("已根据你的需求生成一条");
            if (!destination.isNationwide()) {
                summary.append(destination.getDisplayName());
            }
            summary.append(days).append("天非遗探索建议。");
            summary.append("当前库内暂时没有足够匹配的非遗项目或体验活动，先按偏好方向整理为：");
            summary.append(String.join("、", preferences));
            summary.append("。你可以换更具体的城市，或补充该地区活动数据后再试。");
            return summary.toString();
        }

        summary.append("已为你生成一条");
        if (!destination.isNationwide()) {
            summary.append(destination.getDisplayName());
        }
        summary.append(days).append("天的非遗体验路线，主线围绕");
        summary.append(String.join("、", preferences));
        summary.append("展开。");

        if (!heritages.isEmpty()) {
            summary.append("已优先选出").append(heritages.size()).append("个非遗项目作为线路锚点");
            if (!activities.isEmpty()) {
                summary.append("，并补充").append(activities.size()).append("个可预约体验点");
            }
            summary.append("。");
        } else {
            summary.append("当前没有高匹配非遗主线，先用").append(activities.size()).append("个可预约体验点补足行程。");
        }

        if (heritages.size() + activities.size() < requestedProjectCount) {
            summary.append("当前总共匹配到").append(heritages.size() + activities.size())
                    .append("项，少于你期望的").append(requestedProjectCount).append("项。");
        }
        if (!contents.isEmpty()) {
            summary.append("同时补充了官方内容，方便出发前快速了解背景。");
        }
        return summary.toString();
    }

    private List<Map<String, Object>> buildItinerary(int days,
                                                     List<IntangibleCulturalHeritage> heritages,
                                                     List<Activity> activities,
                                                     List<OfficialContent> contents) {
        List<Map<String, Object>> itinerary = new ArrayList<>();
        List<List<IntangibleCulturalHeritage>> heritageBuckets = distributeHeritages(days, heritages);
        List<List<Activity>> dayBuckets = distributeActivities(days, activities);
        for (int i = 0; i < days; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", i + 1);
            day.put("theme", i == 0 ? "非遗主题导入" : "非遗深度体验");

            List<Map<String, Object>> dayActivities = new ArrayList<>();
            OfficialContent content = i < contents.size() ? contents.get(i) : null;

            if (content != null) {
                Map<String, Object> intro = new HashMap<>();
                intro.put("type", "official");
                intro.put("time", "09:30");
                intro.put("name", content.getTitle());
                intro.put("description", abbreviate(content.getContent(), 72));
                intro.put("tags", Arrays.asList("官方导读", categoryLabel(content.getCategory())));
                intro.put("details", Collections.singletonList(detail("内容类型", categoryLabel(content.getCategory()))));
                dayActivities.add(intro);
            }

            for (IntangibleCulturalHeritage heritage : heritageBuckets.get(i)) {
                Map<String, Object> anchor = new HashMap<>();
                anchor.put("type", "heritage");
                anchor.put("time", dayActivities.isEmpty() ? "10:00" : nextTimeSlot(dayActivities.size()));
                anchor.put("name", heritage.getName());
                anchor.put("description", abbreviate(primaryHeritageDescription(heritage), 88));
                anchor.put("tags", buildHeritageTags(heritage));
                anchor.put("details", Arrays.asList(
                        detail("地区", valueOrDefault(heritage.getRegion(), "待定")),
                        detail("朝代", valueOrDefault(heritage.getDynasty(), "待定")),
                        detail("级别", heritageLevelLabel(heritage.getLevel())),
                        detail("代表性传承人", valueOrDefault(heritage.getRepresentativeInheritor(), "待定"))
                ));
                dayActivities.add(anchor);
            }

            for (Activity activity : dayBuckets.get(i)) {
                Map<String, Object> experience = new HashMap<>();
                experience.put("type", "activity");
                experience.put("time", dayActivities.isEmpty() ? "10:00" : nextTimeSlot(dayActivities.size()));
                experience.put("name", activity.getTitle());
                experience.put("description", abbreviate(activity.getContent(), 88));
                List<String> tags = new ArrayList<>();
                tags.add("可预约体验点");
                tags.addAll(buildActivityTags(activity));
                experience.put("tags", tags);
                experience.put("details", Arrays.asList(
                        detail("省份", valueOrDefault(activity.getLocationProvince(), "待定")),
                        detail("城市", valueOrDefault(activity.getLocationCity(), "待定")),
                        detail("费用", activity.getPrice() == null || activity.getPrice() == 0 ? "免费" : "¥" + (activity.getPrice() / 100.0)),
                        detail("时间", formatDateTime(activity.getStartTime()))
                ));
                experience.put("business", buildBusiness(activity.getMerchantId(), activity.getLocationDetail()));
                dayActivities.add(experience);
            }

            if (dayActivities.isEmpty()) {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("type", "fallback");
                fallback.put("time", "全天");
                fallback.put("name", "自由探索本地非遗空间");
                fallback.put("description", "当前没有更高匹配度的非遗项目或活动，可结合博物馆、非遗馆、传统街区和官方内容做轻量文化漫游。");
                fallback.put("tags", Arrays.asList("自由行", "文化打卡"));
                fallback.put("details", Collections.singletonList(detail("建议", "换更具体的城市，或补充该地区活动数据后再试")));
                dayActivities.add(fallback);
            }

            day.put("activities", dayActivities);
            itinerary.add(day);
        }
        return itinerary;
    }

    private List<List<IntangibleCulturalHeritage>> distributeHeritages(int days, List<IntangibleCulturalHeritage> heritages) {
        List<List<IntangibleCulturalHeritage>> buckets = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            buckets.add(new ArrayList<>());
        }
        if (days <= 0 || heritages.isEmpty()) {
            return buckets;
        }

        for (int i = 0; i < heritages.size(); i++) {
            buckets.get(i % days).add(heritages.get(i));
        }
        return buckets;
    }

    private String buildSummary(DestinationScope destination,
                                int days,
                                int requestedProjectCount,
                                List<String> preferences,
                                List<Activity> activities,
                                List<OfficialContent> contents) {
        StringBuilder summary = new StringBuilder();
        if (activities.isEmpty()) {
            summary.append("已根据你的需求生成一条");
            if (!destination.isNationwide()) {
                summary.append(destination.getDisplayName());
            }
            summary.append(days).append("天非遗探索建议。");
            summary.append("当前库内暂时没有足够匹配的");
            summary.append(destination.isProvince() ? "省内" : destination.isCity() ? "同城" : "");
            summary.append("活动，先给你整理出适合的体验方向：");
            summary.append(String.join("、", preferences));
            summary.append("。你可以换一个更具体的城市，或先补充该地区活动数据后再试。");
            return summary.toString();
        }

        summary.append("已为你生成一条");
        if (!destination.isNationwide()) {
            summary.append(destination.getDisplayName());
        }
        summary.append(days).append("天的非遗体验路线，重点围绕");
        summary.append(String.join("、", preferences));
        summary.append("展开。");

        summary.append("优先安排了更贴合你需求的活动体验。");
        if (activities.size() < requestedProjectCount) {
            summary.append("当前共匹配到").append(activities.size()).append("个项目，少于你期望的").append(requestedProjectCount).append("个项目。");
        }
        if (!contents.isEmpty()) {
            summary.append("同时补充了相关官方内容，方便出发前快速了解背景。");
        }
        return summary.toString();
    }

    private List<Map<String, Object>> buildItinerary(int days, List<Activity> activities, List<OfficialContent> contents) {
        List<Map<String, Object>> itinerary = new ArrayList<>();
        List<List<Activity>> dayBuckets = distributeActivities(days, activities);
        for (int i = 0; i < days; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", i + 1);
            day.put("theme", i == 0 ? "城市导入与非遗认知" : "深度体验与文化漫游");

            List<Map<String, Object>> dayActivities = new ArrayList<>();
            OfficialContent content = i < contents.size() ? contents.get(i) : null;

            if (content != null) {
                Map<String, Object> intro = new HashMap<>();
                intro.put("type", "official");
                intro.put("time", "09:30");
                intro.put("name", content.getTitle());
                intro.put("description", abbreviate(content.getContent(), 72));
                intro.put("tags", Arrays.asList("官方导读", categoryLabel(content.getCategory())));
                intro.put("details", Collections.singletonList(detail("内容类型", categoryLabel(content.getCategory()))));
                dayActivities.add(intro);
            }

            for (Activity activity : dayBuckets.get(i)) {
                Map<String, Object> experience = new HashMap<>();
                experience.put("type", "activity");
                experience.put("time", dayActivities.isEmpty() ? "10:00" : nextTimeSlot(dayActivities.size()));
                experience.put("name", activity.getTitle());
                experience.put("description", abbreviate(activity.getContent(), 88));
                experience.put("tags", buildActivityTags(activity));
                experience.put("details", Arrays.asList(
                        detail("省份", valueOrDefault(activity.getLocationProvince(), "待定")),
                        detail("城市", valueOrDefault(activity.getLocationCity(), "待定")),
                        detail("费用", activity.getPrice() == null || activity.getPrice() == 0 ? "免费" : "¥" + (activity.getPrice() / 100.0)),
                        detail("时间", formatDateTime(activity.getStartTime()))
                ));
                experience.put("business", buildBusiness(activity.getMerchantId(), activity.getLocationDetail()));
                dayActivities.add(experience);
            }

            if (dayActivities.isEmpty()) {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("type", "fallback");
                fallback.put("time", "全天");
                fallback.put("name", "自由探索本地非遗空间");
                fallback.put("description", "当前没有更高匹配度的活动，可先结合城市博物馆、非遗展馆、传统街区和官方内容做一条轻量文化漫游路线。");
                fallback.put("tags", Arrays.asList("自由行", "文化打卡"));
                fallback.put("details", Collections.singletonList(detail("建议", "换一个更具体的城市或补充该地区活动数据后再试")));
                dayActivities.add(fallback);
            }

            day.put("activities", dayActivities);
            itinerary.add(day);
        }
        return itinerary;
    }

    private List<List<Activity>> distributeActivities(int days, List<Activity> activities) {
        List<List<Activity>> buckets = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            buckets.add(new ArrayList<>());
        }
        if (days <= 0 || activities.isEmpty()) {
            return buckets;
        }

        for (int i = 0; i < activities.size(); i++) {
            buckets.get(i % days).add(activities.get(i));
        }
        return buckets;
    }

    private String nextTimeSlot(int index) {
        String[] slots = {"14:00", "16:00", "19:00", "20:30"};
        return slots[Math.min(index - 1, slots.length - 1)];
    }

    private List<Map<String, Object>> buildBusinesses(List<Activity> activities) {
        LinkedHashMap<Long, Map<String, Object>> businesses = new LinkedHashMap<>();
        for (Activity activity : activities) {
            businesses.putIfAbsent(activity.getMerchantId(), businessCard(activity.getMerchantId(), activity.getLocationDetail(), activity.getHeritageType()));
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
            card.put("location", joinLocation(activity.getLocationProvince(), activity.getLocationCity(), activity.getLocationDetail()));
            cards.add(card);
        }
        return cards;
    }


    private List<Map<String, Object>> buildExperienceItinerary(int days, List<Activity> activities) {
        List<Map<String, Object>> itinerary = new ArrayList<>();
        List<List<Activity>> dayBuckets = distributeActivities(days, activities);
        for (int i = 0; i < days; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", i + 1);
            day.put("theme", buildActivityDayTheme(dayBuckets.get(i), i));

            List<Map<String, Object>> dayActivities = new ArrayList<>();
            for (Activity activity : dayBuckets.get(i)) {
                Map<String, Object> experience = new HashMap<>();
                experience.put("type", "activity");
                experience.put("time", dayActivities.isEmpty() ? "10:00" : nextTimeSlot(dayActivities.size()));
                experience.put("name", activity.getTitle());
                experience.put("description", abbreviate(activity.getContent(), 88));
                experience.put("tags", buildActivityTags(activity));
                experience.put("details", Arrays.asList(
                        detail("省份", valueOrDefault(activity.getLocationProvince(), "待定")),
                        detail("城市", valueOrDefault(activity.getLocationCity(), "待定")),
                        detail("费用", activity.getPrice() == null || activity.getPrice() == 0 ? "免费" : "¥" + (activity.getPrice() / 100.0)),
                        detail("时间", formatDateTime(activity.getStartTime()))
                ));
                experience.put("business", buildBusiness(activity.getMerchantId(), activity.getLocationDetail()));
                dayActivities.add(experience);
            }

            if (dayActivities.isEmpty()) {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("type", "fallback");
                fallback.put("time", "全天");
                fallback.put("name", "自由探索");
                fallback.put("description", "当天未找到更匹配的活动。您可以安排博物馆参观、非遗场馆或传统街区漫步。");
                fallback.put("tags", Arrays.asList("自由行", "文化漫步"));
                fallback.put("details", Collections.singletonList(detail("建议", "尝试更具体的城市或为该地区添加更多活动数据。")));
                dayActivities.add(fallback);
            }

            day.put("activities", dayActivities);
            itinerary.add(day);
        }
        return itinerary;
    }

    private String buildActivityDayTheme(List<Activity> activities, int index) {
        if (activities == null || activities.isEmpty()) {
            return "自由探索";
        }

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (Activity activity : activities) {
            if (hasText(activity.getHeritageType())) {
                labels.add(activity.getHeritageType().trim());
            }
            if (labels.size() >= 2) {
                break;
            }
        }
        if (!labels.isEmpty()) {
            return String.join(" + ", labels) + " 路线";
        }
        return "体验路线第 " + (index + 1) + " 天";
    }

    private List<IntangibleCulturalHeritage> selectRelatedHeritages(List<Activity> activities,
                                                                    List<IntangibleCulturalHeritage> prioritizedHeritages,
                                                                    List<IntangibleCulturalHeritage> allHeritages,
                                                                    int limit) {
        if (activities.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        List<IntangibleCulturalHeritage> candidatePool = new ArrayList<>();
        candidatePool.addAll(prioritizedHeritages);
        for (IntangibleCulturalHeritage heritage : allHeritages) {
            boolean exists = candidatePool.stream().anyMatch(item -> item.getId().equals(heritage.getId()));
            if (!exists) {
                candidatePool.add(heritage);
            }
        }

        LinkedHashMap<Long, IntangibleCulturalHeritage> selected = new LinkedHashMap<>();
        for (Activity activity : activities) {
            IntangibleCulturalHeritage match = findBestRelatedHeritage(activity, candidatePool);
            if (match != null) {
                selected.putIfAbsent(match.getId(), match);
            }
            if (selected.size() >= limit) {
                break;
            }
        }

        if (selected.isEmpty()) {
            return prioritizedHeritages.stream()
                    .limit(Math.min(limit, prioritizedHeritages.size()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>(selected.values()).subList(0, Math.min(limit, selected.size()));
    }

    private IntangibleCulturalHeritage findBestRelatedHeritage(Activity activity, List<IntangibleCulturalHeritage> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        List<String> keywords = extractActivityKeywords(activity);
        IntangibleCulturalHeritage best = null;
        int bestScore = 0;
        for (IntangibleCulturalHeritage heritage : candidates) {
            int score = scoreRelatedHeritage(activity, heritage, keywords);
            if (score > bestScore) {
                bestScore = score;
                best = heritage;
            }
        }
        return bestScore > 0 ? best : null;
    }

    private List<String> extractActivityKeywords(Activity activity) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        addKeywordTokens(keywords, activity.getHeritageType());
        addKeywordTokens(keywords, activity.getTitle());
        addKeywordTokens(keywords, activity.getSubtitle());
        return keywords.stream().limit(12).collect(Collectors.toList());
    }

    private int scoreRelatedHeritage(Activity activity, IntangibleCulturalHeritage heritage, List<String> keywords) {
        int score = 0;
        score += matchKeywords(keywords, heritage.getName(), 10);
        score += matchKeywords(keywords, heritage.getCategory(), 7);
        score += matchKeywords(keywords, heritage.getSubcategory(), 7);
        score += matchKeywords(keywords, heritage.getIntroduction(), 3);
        score += matchKeywords(keywords, heritage.getRegion(), 4);
        if (hasText(activity.getHeritageType())) {
            score += matchKeywords(Collections.singletonList(activity.getHeritageType()), heritage.getName(), 12);
            score += matchKeywords(Collections.singletonList(activity.getHeritageType()), heritage.getCategory(), 10);
            score += matchKeywords(Collections.singletonList(activity.getHeritageType()), heritage.getSubcategory(), 10);
        }
        return score;
    }

    private List<Map<String, Object>> buildRelatedHeritageHighlights(List<Activity> activities,
                                                                     List<IntangibleCulturalHeritage> heritages,
                                                                     List<OfficialContent> contents) {
        List<Map<String, Object>> highlights = new ArrayList<>();
        for (IntangibleCulturalHeritage heritage : heritages.subList(0, Math.min(heritages.size(), 4))) {
            Map<String, Object> highlight = new HashMap<>();
            highlight.put("title", heritage.getName());
            highlight.put("category", valueOrDefault(heritage.getCategory(), "非遗主题"));
            highlight.put("summary", abbreviate(primaryHeritageDescription(heritage), 110));
            highlight.put("region", valueOrDefault(heritage.getRegion(), "地区待定"));
            highlight.put("relatedType", matchActivityTypeForHeritage(heritage, activities));
            highlights.add(highlight);
        }
        return highlights;
    }

    private String matchActivityTypeForHeritage(IntangibleCulturalHeritage heritage, List<Activity> activities) {
        for (Activity activity : activities) {
            if (hasText(activity.getHeritageType())) {
                String type = activity.getHeritageType().trim();
                if ((hasText(heritage.getName()) && heritage.getName().contains(type))
                        || (hasText(heritage.getCategory()) && heritage.getCategory().contains(type))
                        || (hasText(heritage.getSubcategory()) && heritage.getSubcategory().contains(type))) {
                    return type;
                }
            }
        }
        return "相关介绍";
    }
    private List<Map<String, Object>> buildOfficialHighlights(List<IntangibleCulturalHeritage> heritages, List<OfficialContent> contents) {
        List<Map<String, Object>> highlights = new ArrayList<>();
        for (IntangibleCulturalHeritage heritage : heritages.subList(0, Math.min(heritages.size(), 3))) {
            Map<String, Object> highlight = new HashMap<>();
            highlight.put("title", heritage.getName());
            highlight.put("category", valueOrDefault(heritage.getCategory(), "非遗项目"));
            highlight.put("summary", abbreviate(primaryHeritageDescription(heritage), 80));
            highlights.add(highlight);
        }
        for (OfficialContent content : contents.subList(0, Math.min(contents.size(), 3))) {
            if (highlights.size() >= 3) {
                break;
            }
            Map<String, Object> highlight = new HashMap<>();
            highlight.put("title", content.getTitle());
            highlight.put("category", categoryLabel(content.getCategory()));
            highlight.put("summary", abbreviate(content.getContent(), 80));
            highlights.add(highlight);
        }
        return highlights;
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

    private String primaryHeritageDescription(IntangibleCulturalHeritage heritage) {
        if (hasText(heritage.getIntroduction())) {
            return heritage.getIntroduction();
        }
        if (hasText(heritage.getInheritanceValue())) {
            return heritage.getInheritanceValue();
        }
        if (hasText(heritage.getHistory())) {
            return heritage.getHistory();
        }
        return "暂无详细介绍";
    }

    private List<String> buildHeritageTags(IntangibleCulturalHeritage heritage) {
        List<String> tags = new ArrayList<>();
        tags.add("非遗主线");
        tags.add(valueOrDefault(heritage.getCategory(), "非遗项目"));
        if (hasText(heritage.getSubcategory())) {
            tags.add(heritage.getSubcategory());
        }
        if (hasText(heritage.getDynasty())) {
            tags.add(heritage.getDynasty());
        }
        return tags;
    }

    private String heritageLevelLabel(String level) {
        if ("national".equals(level)) {
            return "国家级";
        }
        if ("provincial".equals(level)) {
            return "省级";
        }
        if ("municipal".equals(level)) {
            return "市级";
        }
        return valueOrDefault(level, "待定");
    }

    private Map<String, Object> buildBusiness(Long merchantId, String address) {
        Map<String, Object> business = new HashMap<>();
        Optional<User> merchant = userRepository.findById(merchantId);
        business.put("name", merchant.map(u -> hasText(u.getShopName()) ? u.getShopName() : u.getNickname()).orElse("合作商家"));
        business.put("address", valueOrDefault(address, merchant.map(User::getLocation).orElse("以活动详情页为准")));
        business.put("phone", merchant.map(User::getPhone).orElse("站内联系"));
        return business;
    }

    private Map<String, Object> businessCard(Long merchantId, String address, String heritageType) {
        Map<String, Object> card = new HashMap<>();
        Optional<User> merchant = userRepository.findById(merchantId);
        User user = merchant.orElse(null);
        card.put("name", user == null ? "合作商家" : valueOrDefault(user.getShopName(), user.getNickname()));
        card.put("address", valueOrDefault(address, user == null ? "以活动页为准" : valueOrDefault(user.getLocation(), "以活动页为准")));
        card.put("description", user == null ? "提供当地非遗体验与文化服务" : valueOrDefault(user.getShopIntro(), "提供当地非遗体验与文化服务"));
        card.put("tags", Arrays.asList(valueOrDefault(heritageType, "非遗体验"), "推荐商家"));
        return card;
    }

    private List<String> buildActivityTags(Activity activity) {
        List<String> tags = new ArrayList<>();
        tags.add(valueOrDefault(activity.getHeritageType(), inferHeritageType(activity.getTitle())));
        tags.add("signup".equals(activity.getStatus()) ? "可报名" : valueOrDefault(activity.getStatus(), "活动"));
        if (hasText(activity.getActivityType())) {
            tags.add(activity.getActivityType());
        }
        return tags;
    }

    private String inferHeritageType(String title) {
        if (!hasText(title)) {
            return "非遗体验";
        }
        for (Map.Entry<String, List<String>> entry : PREFERENCE_GROUPS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (title.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return "非遗体验";
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

    private String joinLocation(String province, String city, String detail) {
        List<String> parts = new ArrayList<>();
        if (hasText(province)) {
            parts.add(province);
        }
        if (hasText(city) && !city.equals(province)) {
            parts.add(city);
        }
        if (hasText(detail)) {
            parts.add(detail);
        }
        if (parts.isEmpty()) {
            return "地点待定";
        }
        return String.join(" · ", parts);
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "待定";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private String abbreviate(String text, int maxLength) {
        if (!hasText(text)) {
            return "暂无详细介绍";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static Map<String, List<String>> buildPreferenceGroups() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("戏曲表演", Arrays.asList("戏曲", "京剧", "昆曲", "昆山腔", "川剧", "变脸", "脸谱"));
        map.put("刺绣工艺", Arrays.asList("刺绣", "绣", "苏绣", "蜀绣", "广绣", "京绣", "鲁绣", "苗绣"));
        map.put("织造纹样", Arrays.asList("云锦", "宋锦", "壮锦", "缂丝", "织造", "织机", "纹样"));
        map.put("手作体验", Arrays.asList("手作", "工艺", "工坊", "彩绘", "剪纸", "面塑", "扎制", "拉坯", "制扇", "木雕", "雕刻", "制墨", "抄纸", "竹编", "彩塑", "糖画", "传拓"));
        map.put("节庆民俗", Arrays.asList("节庆", "民俗", "庙会", "清明", "端午", "龙舟", "铜鼓"));
        map.put("夜游灯彩", Arrays.asList("夜游", "夜场", "灯彩", "宫灯", "花灯", "秦淮", "中轴"));
        map.put("亲子互动", Arrays.asList("亲子", "彩塑", "风筝", "糖画", "面塑", "兔儿爷"));
        map.put("文化漫游", Arrays.asList("导览", "博物", "展演", "雅集", "闻香"));
        map.put("传统美术", Arrays.asList("美术", "年画", "唐卡", "版画", "皮影", "书画"));
        map.put("少数民族非遗", Arrays.asList("少数民族", "民族", "苗绣", "银饰", "侗族", "苗族"));
        return map;
    }

    private static Map<String, List<String>> buildDynastyGroups() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("唐代", Arrays.asList("唐代", "唐朝", "大唐"));
        map.put("宋代", Arrays.asList("宋代", "宋朝", "两宋"));
        map.put("明代", Arrays.asList("明代", "明朝", "大明"));
        map.put("清代", Arrays.asList("清代", "清朝", "大清"));
        return map;
    }

    private static Map<String, List<String>> buildCategoryGroups() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("传统手工艺", Arrays.asList("传统手工艺", "手工艺", "手工", "工艺", "手作", "文房四宝", "唐三彩", "景泰蓝", "剪纸", "面塑", "糖画"));
        map.put("传统戏曲", Arrays.asList("传统戏曲", "戏曲", "京剧", "昆曲", "川剧", "变脸"));
        map.put("传统美术", Arrays.asList("传统美术", "美术", "美术类", "年画", "唐卡", "皮影", "书画"));
        map.put("传统民俗", Arrays.asList("传统民俗", "民俗", "民俗类", "节俗", "庙会"));
        map.put("少数民族非遗", Arrays.asList("少数民族非遗", "少数民族", "民族非遗"));
        return map;
    }

    private static Map<String, List<String>> buildThemeGroups() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("春节", Arrays.asList("春节", "新春", "过年"));
        map.put("中秋", Arrays.asList("中秋", "中秋节"));
        map.put("清明", Arrays.asList("清明", "清明节"));
        map.put("端午", Arrays.asList("端午", "端午节"));
        map.put("文房四宝", Arrays.asList("文房四宝"));
        return map;
    }

    private static Map<String, List<String>> buildRegionGroups() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("南方", Arrays.asList("江苏", "浙江", "安徽", "广东"));
        return map;
    }

    private static class ScoredActivity {
        private final Activity activity;
        private final int score;

        private ScoredActivity(Activity activity, int score) {
            this.activity = activity;
            this.score = score;
        }

        public Activity getActivity() {
            return activity;
        }

        public int getScore() {
            return score;
        }
    }

    private static class ScoredHeritage {
        private final IntangibleCulturalHeritage heritage;
        private final int score;

        private ScoredHeritage(IntangibleCulturalHeritage heritage, int score) {
            this.heritage = heritage;
            this.score = score;
        }

        public IntangibleCulturalHeritage getHeritage() {
            return heritage;
        }

        public int getScore() {
            return score;
        }
    }

    private static class MatchedLabel {
        private final String label;
        private final int index;

        private MatchedLabel(String label, int index) {
            this.label = label;
            this.index = index;
        }

        public String getLabel() {
            return label;
        }

        public int getIndex() {
            return index;
        }
    }

    private static class DestinationScope {
        private final String type;
        private final String name;

        private DestinationScope(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public static DestinationScope nationwide() {
            return new DestinationScope("nationwide", "全国");
        }

        public static DestinationScope province(String name) {
            return new DestinationScope("province", name);
        }

        public static DestinationScope city(String name) {
            return new DestinationScope("city", name);
        }

        public boolean isNationwide() {
            return "nationwide".equals(type);
        }

        public boolean isProvince() {
            return "province".equals(type);
        }

        public boolean isCity() {
            return "city".equals(type);
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return name;
        }

        public boolean matches(Activity activity) {
            if (isProvince()) {
                return name.equals(activity.getLocationProvince());
            }
            if (isCity()) {
                return name.equals(activity.getLocationCity());
            }
            return true;
        }

        public boolean matches(IntangibleCulturalHeritage heritage) {
            if (heritage == null || heritage.getRegion() == null || heritage.getRegion().trim().isEmpty()) {
                return isNationwide();
            }
            if (isProvince()) {
                return heritage.getRegion().contains(name);
            }
            if (isCity()) {
                return heritage.getRegion().contains(name);
            }
            return true;
        }
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }
}

