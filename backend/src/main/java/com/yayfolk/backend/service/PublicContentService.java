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

import java.util.*;

@Service
public class PublicContentService {

    private final ActivityRepository activityRepository;
    private final ProductRepository productRepository;
    private final OfficialContentRepository officialContentRepository;
    private final UserRepository userRepository;

    public PublicContentService(ActivityRepository activityRepository,
                                ProductRepository productRepository,
                                OfficialContentRepository officialContentRepository,
                                UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.productRepository = productRepository;
        this.officialContentRepository = officialContentRepository;
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> getPublicActivities(String keyword, String city) {
        List<Activity> activities = activityRepository.findByAuditStatusAndStatusNotOrderByStartTimeAsc("approved", "ended");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Activity a : activities) {
            if (keyword != null && !keyword.isEmpty()) {
                boolean match = (a.getTitle() != null && a.getTitle().contains(keyword))
                        || (a.getHeritageType() != null && a.getHeritageType().contains(keyword));
                if (!match) continue;
            }
            if (city != null && !city.isEmpty()) {
                if (a.getLocationCity() == null || !a.getLocationCity().contains(city)) continue;
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
                .orElseThrow(() -> new RuntimeException("活动不存在"));
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
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            if (keyword != null && !keyword.isEmpty()) {
                boolean match = (p.getName() != null && p.getName().contains(keyword))
                        || (p.getHeritageType() != null && p.getHeritageType().contains(keyword));
                if (!match) continue;
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
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        if (!"on_sale".equals(product.getStatus())) {
            throw new RuntimeException("商品已下架");
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
        List<Map<String, Object>> result = new ArrayList<>();
        for (OfficialContent c : list) {
            Map<String, Object> m = new HashMap<>();
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

    private Map<String, Object> activityToMap(Activity a) {
        Map<String, Object> m = new HashMap<>();
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
        Map<String, Object> m = new HashMap<>();
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
