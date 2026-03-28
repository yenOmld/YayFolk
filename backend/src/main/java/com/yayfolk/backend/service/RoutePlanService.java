package com.yayfolk.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.entity.RoutePlan;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.RoutePlanRepository;
import com.yayfolk.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoutePlanService {

    private final RoutePlanRepository routePlanRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public RoutePlanService(RoutePlanRepository routePlanRepository,
                            UserRepository userRepository,
                            ObjectMapper objectMapper) {
        this.routePlanRepository = routePlanRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> saveFavorite(String username, Map<String, Object> payload) {
        User user = getUser(username);
        String query = readString(payload.get("query"));
        if (query.isEmpty()) {
            throw new RuntimeException("收藏路线时缺少查询内容");
        }

        Map<String, Object> route = readMap(payload.get("route"));
        if (route.isEmpty()) {
            throw new RuntimeException("收藏路线时缺少路线结果");
        }

        RoutePlan routePlan = new RoutePlan();
        routePlan.setUserId(user.getId());
        routePlan.setQuery(query);
        routePlan.setMatchedCount(readMatchedCount(route));
        routePlan.setSuggestedDays(readInt(route.get("days"), 1));
        routePlan.setRouteText(readString(route.get("summary")));
        routePlan.setDailyPlans(toJson(route.get("itinerary")));
        routePlan.setBudget(toJson(route.get("budget")));
        routePlan.setTravelTips(toJson(resolveTravelTips(route)));
        routePlan.setParsedQuery(toJson(buildParsedQuery(query, route)));

        RoutePlan saved = routePlanRepository.save(routePlan);
        return toDetail(saved);
    }

    public List<Map<String, Object>> getFavorites(String username) {
        User user = getUser(username);
        List<RoutePlan> routePlans = routePlanRepository.findByUserIdOrderByUpdateTimeDescIdDesc(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (RoutePlan routePlan : routePlans) {
            result.add(toSummary(routePlan));
        }
        return result;
    }

    public Map<String, Object> getFavoriteDetail(String username, Long id) {
        User user = getUser(username);
        RoutePlan routePlan = routePlanRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("收藏路线不存在"));
        return toDetail(routePlan);
    }

    @Transactional
    public void deleteFavorite(String username, Long id) {
        User user = getUser(username);
        RoutePlan routePlan = routePlanRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("收藏路线不存在"));
        routePlanRepository.delete(routePlan);
    }

    private User getUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("未授权，请先登录");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    private Map<String, Object> toSummary(RoutePlan routePlan) {
        Map<String, Object> parsedQuery = parseMap(routePlan.getParsedQuery());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", routePlan.getId());
        summary.put("query", routePlan.getQuery());
        summary.put("destination", readString(parsedQuery.get("destination")));
        summary.put("days", routePlan.getSuggestedDays());
        summary.put("summary", routePlan.getRouteText());
        summary.put("matchedCount", routePlan.getMatchedCount());
        summary.put("createTime", routePlan.getCreateTime());
        summary.put("updateTime", routePlan.getUpdateTime());
        return summary;
    }

    private Map<String, Object> toDetail(RoutePlan routePlan) {
        Map<String, Object> parsedQuery = parseMap(routePlan.getParsedQuery());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", routePlan.getId());
        detail.put("query", routePlan.getQuery());
        detail.put("destination", readString(parsedQuery.get("destination")));
        detail.put("days", routePlan.getSuggestedDays());
        detail.put("preferences", readStringList(parsedQuery.get("preferences")));
        detail.put("summary", routePlan.getRouteText());
        detail.put("itinerary", parseList(routePlan.getDailyPlans()));
        detail.put("recommendedBusinesses", readList(parsedQuery.get("recommendedBusinesses")));
        detail.put("activities", readList(parsedQuery.get("activities")));
        detail.put("officialHighlights", readList(parsedQuery.get("officialHighlights")));
        detail.put("budget", parseJson(routePlan.getBudget()));
        detail.put("travelTips", parseJson(routePlan.getTravelTips()));
        detail.put("matchedCount", routePlan.getMatchedCount());
        detail.put("createTime", routePlan.getCreateTime());
        detail.put("updateTime", routePlan.getUpdateTime());
        return detail;
    }

    private Map<String, Object> buildParsedQuery(String query, Map<String, Object> route) {
        Map<String, Object> parsedQuery = new LinkedHashMap<>();
        parsedQuery.put("query", query);
        parsedQuery.put("destination", route.get("destination"));
        parsedQuery.put("days", route.get("days"));
        parsedQuery.put("preferences", readStringList(route.get("preferences")));
        parsedQuery.put("recommendedBusinesses", readList(route.get("recommendedBusinesses")));
        parsedQuery.put("activities", readList(route.get("activities")));
        parsedQuery.put("officialHighlights", readList(route.get("officialHighlights")));
        return parsedQuery;
    }

    private Object resolveTravelTips(Map<String, Object> route) {
        Object travelTips = route.get("travelTips");
        if (travelTips != null) {
            return travelTips;
        }
        Object officialHighlights = route.get("officialHighlights");
        if (officialHighlights != null) {
            return officialHighlights;
        }
        return Collections.emptyList();
    }

    private int readMatchedCount(Map<String, Object> route) {
        Object matchedCount = route.get("matchedCount");
        if (matchedCount != null) {
            return readInt(matchedCount, 0);
        }
        Object activities = route.get("activities");
        if (activities instanceof List) {
            return ((List<?>) activities).size();
        }
        return 0;
    }

    private String toJson(Object value) {
        try {
            if (value == null) {
                return null;
            }
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("路线收藏序列化失败");
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private List<Map<String, Object>> parseList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> readMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    private List<Map<String, Object>> readList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> source = (List<?>) value;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : source) {
            if (item instanceof Map) {
                result.add(new LinkedHashMap<>((Map<String, Object>) item));
            }
        }
        return result;
    }

    private List<String> readStringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> source = (List<?>) value;
        List<String> result = new ArrayList<>();
        for (Object item : source) {
            if (item != null) {
                String text = item.toString().trim();
                if (!text.isEmpty()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private int readInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String readString(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
