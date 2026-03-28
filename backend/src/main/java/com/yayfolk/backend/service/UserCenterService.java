package com.yayfolk.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.entity.ActivityBooking;
import com.yayfolk.backend.entity.DiscoverPost;
import com.yayfolk.backend.entity.DiscoverPostHistory;
import com.yayfolk.backend.entity.Order;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.entity.UserFollow;
import com.yayfolk.backend.entity.UserProfileVisit;
import com.yayfolk.backend.repository.ActivityBookingRepository;
import com.yayfolk.backend.repository.DiscoverPostHistoryRepository;
import com.yayfolk.backend.repository.DiscoverPostRepository;
import com.yayfolk.backend.repository.OrderRepository;
import com.yayfolk.backend.repository.UserFollowRepository;
import com.yayfolk.backend.repository.UserProfileVisitRepository;
import com.yayfolk.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserCenterService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ActivityBookingRepository activityBookingRepository;
    private final DiscoverPostRepository discoverPostRepository;
    private final DiscoverPostHistoryRepository historyRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserProfileVisitRepository userProfileVisitRepository;
    private final ObjectMapper objectMapper;

    public UserCenterService(UserRepository userRepository,
                             OrderRepository orderRepository,
                             ActivityBookingRepository activityBookingRepository,
                             DiscoverPostRepository discoverPostRepository,
                             DiscoverPostHistoryRepository historyRepository,
                             UserFollowRepository userFollowRepository,
                             UserProfileVisitRepository userProfileVisitRepository,
                             ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.activityBookingRepository = activityBookingRepository;
        this.discoverPostRepository = discoverPostRepository;
        this.historyRepository = historyRepository;
        this.userFollowRepository = userFollowRepository;
        this.userProfileVisitRepository = userProfileVisitRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getAchievements(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        List<Order> orders = orderRepository.findByUserIdAndDeleteStatusOrderByCreateTimeDesc(user.getId(), 0);
        List<ActivityBooking> bookings = activityBookingRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
        List<DiscoverPost> posts = discoverPostRepository.findByUserIdAndStatusOrderByCreateTimeDesc(user.getId(), 1);
        List<DiscoverPostHistory> histories = historyRepository.findByUserIdOrderByLastViewTimeDesc(user.getId());

        long productOrderCount = orders.size();
        long bookingCount = bookings.size();
        long checkedInCount = bookings.stream().filter(item -> "checked_in".equals(item.getStatus())).count();
        long postCount = posts.size();
        long partnerPostCount = posts.stream().filter(item -> "partner".equals(item.getCategory())).count();
        long historyCount = histories.size();

        List<Map<String, Object>> badges = new ArrayList<>();
        badges.add(buildBadge("first-order", "非遗初体验", "完成第一笔非遗商品订单", productOrderCount, 1, "order"));
        badges.add(buildBadge("collector", "文化收藏家", "累计完成 3 笔商品订单", productOrderCount, 3, "order"));
        badges.add(buildBadge("first-booking", "活动报名官", "完成第一次非遗活动报名", bookingCount, 1, "activity"));
        badges.add(buildBadge("check-in", "线下打卡者", "完成 1 次到店核销或签到", checkedInCount, 1, "checkin"));
        badges.add(buildBadge("deep-explorer", "非遗深度体验官", "累计完成 3 次线下打卡", checkedInCount, 3, "checkin"));
        badges.add(buildBadge("storyteller", "分享记录者", "发布 1 篇非遗内容帖", postCount, 1, "post"));
        badges.add(buildBadge("partner-host", "搭子召集人", "发布 1 篇组团搭子帖", partnerPostCount, 1, "partner"));
        badges.add(buildBadge("wanderer", "文化漫游者", "累计浏览 10 条非遗内容", historyCount, 10, "history"));

        long unlockedCount = badges.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("unlocked")))
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("productOrders", productOrderCount);
        summary.put("activityBookings", bookingCount);
        summary.put("checkedInCount", checkedInCount);
        summary.put("posts", postCount);
        summary.put("partnerPosts", partnerPostCount);
        summary.put("historyCount", historyCount);
        summary.put("unlockedCount", unlockedCount);
        summary.put("totalCount", badges.size());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("badges", badges);
        return result;
    }

    public Map<String, Object> getUserHomepage(String viewerUsername, Long profileUserId) {
        User profileUser = userRepository.findById(profileUserId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        recordProfileVisit(viewerUsername, profileUser);

        List<DiscoverPost> posts = discoverPostRepository.findByUserIdAndStatusOrderByCreateTimeDesc(profileUserId, 1)
                .stream()
                .filter(post -> "passed".equalsIgnoreCase(post.getAuditStatus()))
                .collect(Collectors.toList());

        Map<String, Object> achievements = getAchievements(profileUser.getUsername());
        Map<String, Object> achievementSummary = castMap(achievements.get("summary"));
        List<?> badgeList = achievements.get("badges") instanceof List ? (List<?>) achievements.get("badges") : Collections.emptyList();

        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", profileUser.getId());
        userMap.put("username", defaultString(profileUser.getUsername()));
        userMap.put("nickname", displayName(profileUser));
        userMap.put("avatar", avatarOf(profileUser));
        userMap.put("bio", defaultString(profileUser.getBio()));
        userMap.put("role", defaultString(profileUser.getRole()));
        userMap.put("country", defaultString(profileUser.getCountry()));
        userMap.put("location", buildLocation(profileUser));
        userMap.put("followerCount", safeInt(profileUser.getFollowerCount()));
        userMap.put("followingCount", safeInt(profileUser.getFollowingCount()));
        userMap.put("shopName", defaultString(profileUser.getShopName()));
        userMap.put("shopIntro", defaultString(profileUser.getShopIntro()));
        userMap.put("shopCover", defaultString(profileUser.getShopCover()));
        userMap.put("shopStatus", defaultString(profileUser.getShopStatus()));
        userMap.put("isMerchant", isMerchant(profileUser));
        userMap.put("createTime", formatDate(profileUser.getCreateTime()));
        userMap.put("isFollowing", isFollowing(viewerUsername, profileUser.getId()));

        long totalViews = posts.stream().map(DiscoverPost::getViewCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long totalCollects = posts.stream().map(DiscoverPost::getCollectCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long totalComments = posts.stream().map(DiscoverPost::getCommentCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("postCount", posts.size());
        summary.put("totalViews", totalViews);
        summary.put("totalCollects", totalCollects);
        summary.put("totalComments", totalComments);
        summary.put("unlockedBadgeCount", safeLong(achievementSummary.get("unlockedCount")));
        summary.put("badgeCount", badgeList.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isCurrentUser", StringUtils.hasText(viewerUsername) && viewerUsername.equals(profileUser.getUsername()));
        result.put("user", userMap);
        result.put("summary", summary);
        result.put("posts", posts.stream().map(post -> toHomepagePostSummary(post, profileUser)).collect(Collectors.toList()));
        result.put("badges", badgeList);
        result.put("achievementSummary", achievementSummary);
        return result;
    }

    public Map<String, Object> getHomepageSettings(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", defaultString(user.getUsername()));
        result.put("nickname", displayName(user));
        result.put("avatar", avatarOf(user));
        result.put("role", defaultString(user.getRole()));
        result.put("bio", defaultString(user.getBio()));
        result.put("location", defaultString(user.getLocation()));
        result.put("shopName", defaultString(user.getShopName()));
        result.put("shopIntro", defaultString(user.getShopIntro()));
        result.put("shopCover", defaultString(user.getShopCover()));
        result.put("isMerchant", isMerchant(user));
        return result;
    }

    @Transactional
    public Map<String, Object> followUser(String username, Long targetUserId) {
        User follower = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        if (follower.getId().equals(following.getId())) {
            throw new RuntimeException("You cannot follow yourself");
        }

        if (!userFollowRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())) {
            UserFollow relation = new UserFollow();
            relation.setFollowerId(follower.getId());
            relation.setFollowingId(following.getId());
            userFollowRepository.save(relation);
        }

        refreshFollowCounts(follower);
        refreshFollowCounts(following);

        return buildFollowState(follower.getId(), following.getId());
    }

    @Transactional
    public Map<String, Object> unfollowUser(String username, Long targetUserId) {
        User follower = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        if (follower.getId().equals(following.getId())) {
            throw new RuntimeException("You cannot unfollow yourself");
        }

        userFollowRepository.deleteByFollowerIdAndFollowingId(follower.getId(), following.getId());
        refreshFollowCounts(follower);
        refreshFollowCounts(following);

        return buildFollowState(follower.getId(), following.getId());
    }

    public Map<String, Object> getFollowStatus(String username, Long targetUserId) {
        User follower = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        return buildFollowState(follower.getId(), targetUserId);
    }

    public Map<String, Object> getVisitorRecords(String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        List<UserProfileVisit> viewedRecords = userProfileVisitRepository.findByViewerIdOrderByUpdateTimeDesc(currentUser.getId());
        List<UserProfileVisit> receivedRecords = userProfileVisitRepository.findByProfileUserIdOrderByUpdateTimeDesc(currentUser.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("viewedUsers", buildVisitorItems(viewedRecords, true));
        result.put("receivedVisitors", buildVisitorItems(receivedRecords, false));
        result.put("viewedCount", viewedRecords.size());
        result.put("receivedCount", receivedRecords.size());
        return result;
    }

    private Map<String, Object> buildBadge(String code, String name, String description,
                                           long current, long target, String type) {
        Map<String, Object> badge = new LinkedHashMap<>();
        badge.put("code", code);
        badge.put("name", name);
        badge.put("description", description);
        badge.put("type", type);
        badge.put("current", current);
        badge.put("target", target);
        badge.put("progress", Math.min(current, target));
        badge.put("unlocked", current >= target);
        return badge;
    }

    private Map<String, Object> toHomepagePostSummary(DiscoverPost post, User author) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", post.getId());
        item.put("title", defaultString(post.getTitle()));
        item.put("content", defaultString(post.getContent()));
        item.put("category", defaultString(post.getCategory()));
        item.put("images", readStringArray(post.getImages()));
        item.put("hashtags", readStringArray(post.getTags()));
        item.put("collects", safeInt(post.getCollectCount()));
        item.put("comments", safeInt(post.getCommentCount()));
        item.put("viewCount", safeInt(post.getViewCount()));
        item.put("time", formatDate(post.getCreateTime()));

        Map<String, Object> authorMap = new LinkedHashMap<>();
        authorMap.put("id", author == null ? null : author.getId());
        authorMap.put("name", displayName(author));
        authorMap.put("avatar", avatarOf(author));
        authorMap.put("location", buildLocation(author));
        item.put("author", authorMap);
        return item;
    }

    private List<String> readStringArray(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    private String displayName(User user) {
        if (user == null) {
            return "Unknown";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return defaultString(user.getUsername());
    }

    private String avatarOf(User user) {
        if (user == null || !StringUtils.hasText(user.getAvatar())) {
            return "https://api.dicebear.com/7.x/avataaars/svg?seed=travelate-user";
        }
        return user.getAvatar();
    }

    private String buildLocation(User user) {
        if (user == null) {
            return "";
        }
        if (StringUtils.hasText(user.getLocation())) {
            return user.getLocation();
        }
        return defaultString(user.getCountry());
    }

    private boolean isMerchant(User user) {
        if (user == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(user.getIsMerchant())) {
            return true;
        }
        String shopStatus = defaultString(user.getShopStatus());
        if ("approved".equalsIgnoreCase(shopStatus) || "active".equalsIgnoreCase(shopStatus)) {
            return true;
        }
        String role = defaultString(user.getRole());
        return "merchant".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
    }

    private boolean isFollowing(String viewerUsername, Long targetUserId) {
        if (!StringUtils.hasText(viewerUsername) || targetUserId == null) {
            return false;
        }
        User viewer = userRepository.findByUsername(viewerUsername).orElse(null);
        if (viewer == null || viewer.getId().equals(targetUserId)) {
            return false;
        }
        return userFollowRepository.existsByFollowerIdAndFollowingId(viewer.getId(), targetUserId);
    }

    private void recordProfileVisit(String viewerUsername, User profileUser) {
        if (!StringUtils.hasText(viewerUsername) || profileUser == null) {
            return;
        }

        User viewer = userRepository.findByUsername(viewerUsername).orElse(null);
        if (viewer == null || viewer.getId().equals(profileUser.getId())) {
            return;
        }

        UserProfileVisit visit = userProfileVisitRepository
                .findByViewerIdAndProfileUserId(viewer.getId(), profileUser.getId())
                .orElseGet(UserProfileVisit::new);
        visit.setViewerId(viewer.getId());
        visit.setProfileUserId(profileUser.getId());
        visit.setUpdateTime(new java.util.Date());
        userProfileVisitRepository.save(visit);
    }

    private List<Map<String, Object>> buildVisitorItems(List<UserProfileVisit> records, boolean viewedMode) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = records.stream()
                .map(item -> viewedMode ? item.getProfileUserId() : item.getViewerId())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, item -> item));

        return records.stream()
                .map(record -> {
                    Long targetId = viewedMode ? record.getProfileUserId() : record.getViewerId();
                    User targetUser = userMap.get(targetId);
                    if (targetUser == null) {
                        return null;
                    }

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", targetUser.getId());
                    item.put("username", defaultString(targetUser.getUsername()));
                    item.put("nickname", displayName(targetUser));
                    item.put("avatar", avatarOf(targetUser));
                    item.put("bio", defaultString(targetUser.getBio()));
                    item.put("role", defaultString(targetUser.getRole()));
                    item.put("visitTime", formatDateTime(record.getUpdateTime()));
                    item.put("viewTime", formatDateTime(record.getUpdateTime()));
                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void refreshFollowCounts(User user) {
        long followerCount = userFollowRepository.countByFollowingId(user.getId());
        long followingCount = userFollowRepository.countByFollowerId(user.getId());
        user.setFollowerCount((int) followerCount);
        user.setFollowingCount((int) followingCount);
        userRepository.save(user);
    }

    private Map<String, Object> buildFollowState(Long followerId, Long targetUserId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("following", userFollowRepository.existsByFollowerIdAndFollowingId(followerId, targetUserId));
        result.put("followerCount", userFollowRepository.countByFollowingId(targetUserId));
        result.put("followingCount", userFollowRepository.countByFollowerId(followerId));
        return result;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String formatDateTime(java.util.Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }
}
