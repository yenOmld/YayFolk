package com.yayfolk.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.DiscoverPost;
import com.yayfolk.backend.entity.IntangibleCulturalHeritage;
import com.yayfolk.backend.entity.OfficialContent;
import com.yayfolk.backend.entity.Product;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.DiscoverPostRepository;
import com.yayfolk.backend.repository.IntangibleCulturalHeritageRepository;
import com.yayfolk.backend.repository.OfficialContentRepository;
import com.yayfolk.backend.repository.ProductRepository;
import com.yayfolk.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PublicContentService {
    private static final String HOMEPAGE_ACTIVITY_CATEGORY = "homepage_activity_selection";
    private static final String HOMEPAGE_HERITAGE_CATEGORY = "homepage_heritage_selection";
    private static final String HOMEPAGE_WORK_CATEGORY = "homepage_work_selection";
    private static final int HOMEPAGE_ACTIVITY_LIMIT = 3;
    private static final int HOMEPAGE_HERITAGE_LIMIT = 6;
    private static final int HOMEPAGE_WORK_LIMIT = 6;
    private static final List<String> APPROVED_POST_AUDIT_STATUSES = Arrays.asList("passed", "approved");

    private final ActivityRepository activityRepository;
    private final ProductRepository productRepository;
    private final OfficialContentRepository officialContentRepository;
    private final UserRepository userRepository;
    private final IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository;
    private final DiscoverPostRepository discoverPostRepository;
    private final ObjectMapper objectMapper;

    public PublicContentService(ActivityRepository activityRepository,
                                ProductRepository productRepository,
                                OfficialContentRepository officialContentRepository,
                                UserRepository userRepository,
                                IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository,
                                DiscoverPostRepository discoverPostRepository,
                                ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.productRepository = productRepository;
        this.officialContentRepository = officialContentRepository;
        this.userRepository = userRepository;
        this.intangibleCulturalHeritageRepository = intangibleCulturalHeritageRepository;
        this.discoverPostRepository = discoverPostRepository;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> getPublicActivities(String keyword, String city) {
        List<Activity> activities = activityRepository.findByAuditStatusAndStatusNotOrderByStartTimeAsc("approved", "ended");
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Activity a : activities) {
            if (keyword != null && !keyword.isEmpty()) {
                boolean match = (a.getTitle() != null && a.getTitle().contains(keyword))
                        || (a.getHeritageType() != null && a.getHeritageType().contains(keyword));
                if (!match) {
                    continue;
                }
            }
            if (city != null && !city.isEmpty()) {
                if (a.getLocationCity() == null || !a.getLocationCity().contains(city)) {
                    continue;
                }
            }
            Map<String, Object> m = activityToMap(a);
            userRepository.findById(a.getMerchantId()).ifPresent(u -> {
                m.put("merchantName", u.getShopName() != null ? u.getShopName() : u.getNickname());
                m.put("merchantAvatar", u.getAvatar());
                m.put("merchantIntro", u.getShopIntro());
            });
            result.add(m);
        }
        return result;
    }

    public Map<String, Object> getPublicActivityDetail(Long id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));
        if (!"approved".equals(activity.getAuditStatus())) {
            throw new RuntimeException("Activity has not been approved yet");
        }
        Map<String, Object> result = activityToMap(activity);
        userRepository.findById(activity.getMerchantId()).ifPresent(u -> {
            result.put("merchantName", u.getShopName() != null ? u.getShopName() : u.getNickname());
            result.put("merchantAvatar", u.getAvatar());
            result.put("merchantIntro", u.getShopIntro());
            result.put("merchantCover", u.getShopCover());
        });
        return result;
    }

    public List<Map<String, Object>> getPublicProducts(String keyword) {
        List<Product> products = productRepository.findByStatusOrderByCreateTimeDesc("on_sale");
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Product p : products) {
            if (keyword != null && !keyword.isEmpty()) {
                boolean match = (p.getName() != null && p.getName().contains(keyword))
                        || (p.getHeritageType() != null && p.getHeritageType().contains(keyword));
                if (!match) {
                    continue;
                }
            }
            Map<String, Object> m = productToMap(p);
            userRepository.findById(p.getMerchantId()).ifPresent(u -> {
                m.put("merchantName", u.getShopName() != null ? u.getShopName() : u.getNickname());
                m.put("merchantAvatar", u.getAvatar());
                m.put("merchantIntro", u.getShopIntro());
            });
            result.add(m);
        }
        return result;
    }

    public Map<String, Object> getPublicProductDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product does not exist"));
        if (!"on_sale".equals(product.getStatus())) {
            throw new RuntimeException("Product is no longer on sale");
        }
        Map<String, Object> result = productToMap(product);
        userRepository.findById(product.getMerchantId()).ifPresent(u -> {
            result.put("merchantName", u.getShopName() != null ? u.getShopName() : u.getNickname());
            result.put("merchantAvatar", u.getAvatar());
            result.put("merchantIntro", u.getShopIntro());
            result.put("merchantCover", u.getShopCover());
        });
        return result;
    }

    public List<Map<String, Object>> getOfficialContents(String category) {
        List<OfficialContent> list;
        if (category != null && !category.isEmpty()) {
            list = officialContentRepository.findByIsPublicAndCategoryOrderByCreateTimeDesc(1, category);
        } else {
            list = officialContentRepository.findByIsPublicOrderByCreateTimeDesc(1);
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (OfficialContent c : list) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("content", c.getContent());
            m.put("category", c.getCategory());
            m.put("coverImage", c.getCoverImage());
            m.put("viewCount", c.getViewCount());
            m.put("createTime", c.getCreateTime());
            result.add(m);
        }
        return result;
    }

    public Map<String, Object> getHomepageOfficialContents() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("activities", buildHomepageActivities());
        result.put("heritages", buildHomepageHeritages());
        result.put("works", buildHomepageWorks());
        return result;
    }

    private List<Map<String, Object>> buildHomepageActivities() {
        List<Map<String, Object>> candidates = getPublicActivities(null, null);
        List<Long> selectedIds = readSelectionIds(HOMEPAGE_ACTIVITY_CATEGORY);
        return pickByIds(selectedIds, candidates, HOMEPAGE_ACTIVITY_LIMIT);
    }

    private List<Map<String, Object>> buildHomepageHeritages() {
        List<IntangibleCulturalHeritage> heritageList = intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc();
        List<Map<String, Object>> candidates = new ArrayList<Map<String, Object>>();
        for (IntangibleCulturalHeritage heritage : heritageList) {
            candidates.add(heritageToMap(heritage));
        }
        List<Long> selectedIds = readSelectionIds(HOMEPAGE_HERITAGE_CATEGORY);
        return pickByIds(selectedIds, candidates, HOMEPAGE_HERITAGE_LIMIT);
    }

    private List<Map<String, Object>> buildHomepageWorks() {
        List<DiscoverPost> posts = discoverPostRepository.findByStatusAndAuditStatusInOrderByCreateTimeDesc(1, APPROVED_POST_AUDIT_STATUSES);
        List<Map<String, Object>> candidates = new ArrayList<Map<String, Object>>();
        for (DiscoverPost post : posts) {
            candidates.add(workToMap(post));
        }
        Collections.sort(candidates, (left, right) -> Integer.valueOf(valueAsInt(right.get("heat"))).compareTo(valueAsInt(left.get("heat"))));
        if (candidates.size() > 20) {
            candidates = new ArrayList<Map<String, Object>>(candidates.subList(0, 20));
        }
        List<Long> selectedIds = readSelectionIds(HOMEPAGE_WORK_CATEGORY);
        return pickByIds(selectedIds, candidates, HOMEPAGE_WORK_LIMIT);
    }

    private List<Map<String, Object>> pickByIds(List<Long> selectedIds, List<Map<String, Object>> candidates, int limit) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<Map<String, Object>>();
        }

        if (selectedIds == null || selectedIds.isEmpty()) {
            return new ArrayList<Map<String, Object>>(candidates.subList(0, Math.min(limit, candidates.size())));
        }

        Map<Long, Map<String, Object>> indexed = new LinkedHashMap<Long, Map<String, Object>>();
        for (Map<String, Object> candidate : candidates) {
            Long id = valueAsLong(candidate.get("id"));
            if (id != null) {
                indexed.put(id, candidate);
            }
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Long id : selectedIds) {
            Map<String, Object> item = indexed.get(id);
            if (item != null) {
                result.add(item);
            }
            if (result.size() >= limit) {
                break;
            }
        }

        if (result.isEmpty()) {
            return new ArrayList<Map<String, Object>>(candidates.subList(0, Math.min(limit, candidates.size())));
        }
        return result;
    }

    private List<Long> readSelectionIds(String category) {
        OfficialContent content = officialContentRepository.findTopByCategoryAndIsPublicOrderByCreateTimeDesc(category, 1);
        if (content == null || content.getContent() == null || content.getContent().trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(content.getContent(), new TypeReference<List<Long>>() { });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> heritageToMap(IntangibleCulturalHeritage heritage) {
        Map<String, Object> m = new HashMap<String, Object>();
        List<String> imageList = parseStringList(heritage.getImages());
        m.put("id", heritage.getId());
        m.put("name", heritage.getName());
        m.put("title", heritage.getName());
        m.put("category", heritage.getCategory());
        m.put("subcategory", heritage.getSubcategory());
        m.put("dynasty", heritage.getDynasty());
        m.put("region", heritage.getRegion());
        m.put("level", heritage.getLevel());
        m.put("introduction", heritage.getIntroduction());
        m.put("description", heritage.getIntroduction());
        m.put("history", heritage.getHistory());
        m.put("inheritanceValue", heritage.getInheritanceValue());
        m.put("representativeInheritor", heritage.getRepresentativeInheritor());
        m.put("images", heritage.getImages());
        m.put("imageList", imageList);
        m.put("coverImage", imageList.isEmpty() ? null : imageList.get(0));
        m.put("videoUrl", heritage.getVideoUrl());
        m.put("relatedPoems", parseStringList(heritage.getRelatedPoems()));
        m.put("relatedSolarTerms", parseStringList(heritage.getRelatedSolarTerms()));
        m.put("latitude", heritage.getLatitude());
        m.put("longitude", heritage.getLongitude());
        m.put("isFeatured", heritage.getIsFeatured());
        m.put("viewCount", heritage.getViewCount());
        m.put("createTime", heritage.getCreateTime());
        m.put("updateTime", heritage.getUpdateTime());
        return m;
    }

    private Map<String, Object> workToMap(DiscoverPost post) {
        Map<String, Object> m = new HashMap<String, Object>();
        List<String> imageList = parseStringList(post.getImages());
        m.put("id", post.getId());
        m.put("title", post.getTitle());
        m.put("description", post.getContent());
        m.put("content", post.getContent());
        m.put("coverImage", imageList.isEmpty() ? null : imageList.get(0));
        m.put("images", imageList);
        m.put("viewCount", valueAsInt(post.getViewCount()));
        m.put("collectCount", valueAsInt(post.getCollectCount()));
        m.put("commentCount", valueAsInt(post.getCommentCount()));
        m.put("heat", calculatePostHeat(post));
        m.put("createTime", post.getCreateTime());
        m.put("auditStatus", post.getAuditStatus());
        return m;
    }

    private List<String> parseStringList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<String>();
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = objectMapper.readValue(trimmed, new TypeReference<List<String>>() { });
                List<String> result = new ArrayList<String>();
                for (String item : parsed) {
                    if (item != null && !item.trim().isEmpty()) {
                        result.add(item.trim());
                    }
                }
                return result;
            } catch (Exception ignored) {
            }
        }

        return new ArrayList<String>(Collections.singletonList(trimmed));
    }

    private int calculatePostHeat(DiscoverPost post) {
        return valueAsInt(post.getViewCount()) + valueAsInt(post.getCollectCount()) + valueAsInt(post.getCommentCount());
    }

    private Long valueAsLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private int valueAsInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> activityToMap(Activity a) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", a.getId());
        m.put("merchantId", a.getMerchantId());
        m.put("categoryId", a.getCategoryId());
        m.put("title", a.getTitle());
        m.put("subtitle", a.getSubtitle());
        m.put("coverImage", a.getCoverImage());
        m.put("images", a.getImages());
        m.put("heritageType", a.getHeritageType());
        m.put("activityType", a.getActivityType());
        m.put("startTime", a.getStartTime());
        m.put("endTime", a.getEndTime());
        m.put("price", a.getPrice());
        m.put("maxParticipants", a.getMaxParticipants());
        m.put("currentParticipants", a.getCurrentParticipants());
        m.put("locationProvince", a.getLocationProvince());
        m.put("locationCity", a.getLocationCity());
        m.put("locationDistrict", a.getLocationDistrict());
        m.put("locationDetail", a.getLocationDetail());
        m.put("status", a.getStatus());
        m.put("auditStatus", a.getAuditStatus());
        m.put("auditRemark", a.getAuditRemark());
        m.put("content", a.getContent());
        return m;
    }

    private Map<String, Object> productToMap(Product p) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("subtitle", p.getSubtitle());
        m.put("description", p.getDescription());
        m.put("price", p.getPrice());
        m.put("originalPrice", p.getOriginalPrice());
        m.put("stock", p.getStock());
        m.put("sales", p.getSales());
        m.put("mainImage", p.getMainImage());
        m.put("images", p.getImages());
        m.put("detail", p.getDetail());
        m.put("heritageType", p.getHeritageType());
        m.put("status", p.getStatus());
        return m;
    }
}

