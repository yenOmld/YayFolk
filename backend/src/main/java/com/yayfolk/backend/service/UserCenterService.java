package com.yayfolk.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.entity.ActivityBooking;
import com.yayfolk.backend.entity.DiscoverPost;
import com.yayfolk.backend.entity.DiscoverPostCollection;
import com.yayfolk.backend.entity.DiscoverPostHistory;
import com.yayfolk.backend.entity.Order;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.entity.UserFollow;
import com.yayfolk.backend.entity.UserProfileVisit;
import com.yayfolk.backend.repository.ActivityBookingRepository;
import com.yayfolk.backend.repository.DiscoverPostCollectionRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserCenterService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ActivityBookingRepository activityBookingRepository;
    private final DiscoverPostRepository discoverPostRepository;
    private final DiscoverPostCollectionRepository discoverPostCollectionRepository;
    private final DiscoverPostHistoryRepository historyRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserProfileVisitRepository userProfileVisitRepository;
    private final ObjectMapper objectMapper;

    public UserCenterService(UserRepository userRepository,
                             OrderRepository orderRepository,
                             ActivityBookingRepository activityBookingRepository,
                             DiscoverPostRepository discoverPostRepository,
                             DiscoverPostCollectionRepository discoverPostCollectionRepository,
                             DiscoverPostHistoryRepository historyRepository,
                             UserFollowRepository userFollowRepository,
                             UserProfileVisitRepository userProfileVisitRepository,
                             ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.activityBookingRepository = activityBookingRepository;
        this.discoverPostRepository = discoverPostRepository;
        this.discoverPostCollectionRepository = discoverPostCollectionRepository;
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
        badges.add(buildBadge("first-order", "First Order", "Complete your first product order", productOrderCount, 1, "order"));
        badges.add(buildBadge("collector", "Collector", "Complete 3 product orders", productOrderCount, 3, "order"));
        badges.add(buildBadge("first-booking", "First Booking", "Join your first activity", bookingCount, 1, "activity"));
        badges.add(buildBadge("check-in", "Check-in", "Complete your first offline check-in", checkedInCount, 1, "checkin"));
        badges.add(buildBadge("deep-explorer", "Deep Explorer", "Complete 3 offline check-ins", checkedInCount, 3, "checkin"));
        badges.add(buildBadge("storyteller", "Storyteller", "Publish your first post", postCount, 1, "post"));
        badges.add(buildBadge("partner-host", "Partner Host", "Publish your first partner post", partnerPostCount, 1, "partner"));
        badges.add(buildBadge("wanderer", "Wanderer", "Browse 10 posts", historyCount, 10, "history"));

        long unlockedCount = badges.stream().filter(item -> Boolean.TRUE.equals(item.get("unlocked"))).count();

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

        Long viewerUserId = findUserIdByUsername(viewerUsername);
        boolean isCurrentUser = viewerUserId != null && viewerUserId.equals(profileUserId);
        boolean collectionsVisible = isCurrentUser || !"private".equalsIgnoreCase(defaultString(profileUser.getCollectionVisibility()));

        List<DiscoverPost> posts = getHomepageVisiblePosts(profileUserId, viewerUserId);
        Map<String, Object> achievements = getAchievements(profileUser.getUsername());
        Map<String, Object> achievementSummary = castMap(achievements.get("summary"));
        List<?> badgeList = achievements.get("badges") instanceof List ? (List<?>) achievements.get("badges") : Collections.emptyList();
        List<Map<String, Object>> collections = buildHomepageCollections(profileUserId, viewerUserId, collectionsVisible);
        long collectedCount = getCollectedCount(profileUserId, viewerUserId);

        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", profileUser.getId());
        userMap.put("username", defaultString(profileUser.getUsername()));
        userMap.put("nickname", displayName(profileUser));
        userMap.put("avatar", avatarOf(profileUser));
        userMap.put("bio", defaultString(profileUser.getBio()));
        userMap.put("signature", defaultString(profileUser.getSignature()));
        userMap.put("coverPhoto", resolveHomepageCover(profileUser));
        userMap.put("role", defaultString(profileUser.getRole()));
        userMap.put("country", defaultString(profileUser.getCountry()));
        userMap.put("location", buildLocation(profileUser));
        userMap.put("followerCount", safeInt(profileUser.getFollowerCount()));
        userMap.put("followingCount", safeInt(profileUser.getFollowingCount()));
        userMap.put("collectedCount", collectedCount);
        userMap.put("collectionVisibility", normalizeVisibility(profileUser.getCollectionVisibility()));
        userMap.put("collectionsVisible", collectionsVisible);
        userMap.put("shopName", defaultString(profileUser.getShopName()));
        userMap.put("shopIntro", defaultString(profileUser.getShopIntro()));
        userMap.put("shopCover", defaultString(profileUser.getShopCover()));
        userMap.put("shopStatus", defaultString(profileUser.getShopStatus()));
        userMap.put("isMerchant", isMerchant(profileUser));
        userMap.put("merchantKeyword", defaultString(profileUser.getShopName()));
        userMap.put("merchantRating", null);
        userMap.put("merchantReviewCount", 0);
        userMap.put("createTime", formatDate(profileUser.getCreateTime()));
        userMap.put("isFollowing", isFollowing(viewerUsername, profileUser.getId()));

        long totalViews = posts.stream().map(DiscoverPost::getViewCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long totalCollects = posts.stream().map(DiscoverPost::getCollectCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long totalComments = posts.stream().map(DiscoverPost::getCommentCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("postCount", posts.size());
        summary.put("collectionCount", collections.size());
        summary.put("collectedCount", collectedCount);
        summary.put("totalViews", totalViews);
        summary.put("totalCollects", totalCollects);
        summary.put("totalComments", totalComments);
        summary.put("unlockedBadgeCount", safeLong(achievementSummary.get("unlockedCount")));
        summary.put("badgeCount", badgeList.size());
        summary.put("activityCount", 0);
        summary.put("reviewCount", 0);
        summary.put("averageScore", null);

        Map<String, Object> reviewSummary = new LinkedHashMap<>();
        reviewSummary.put("averageScore", null);
        reviewSummary.put("reviewCount", 0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isCurrentUser", isCurrentUser);
        result.put("user", userMap);
        result.put("summary", summary);
        result.put("posts", posts.stream().map(post -> toHomepagePostSummary(post, profileUser)).collect(Collectors.toList()));
        result.put("collections", collections);
        result.put("badges", badgeList);
        result.put("activities", Collections.emptyList());
        result.put("reviews", Collections.emptyList());
        result.put("reviewSummary", reviewSummary);
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
        result.put("signature", defaultString(user.getSignature()));
        result.put("coverPhoto", resolveHomepageCover(user));
        result.put("location", defaultString(user.getLocation()));
        result.put("shopName", defaultString(user.getShopName()));
        result.put("shopIntro", defaultString(user.getShopIntro()));
        result.put("shopCover", defaultString(user.getShopCover()));
        result.put("collectionVisibility", normalizeVisibility(user.getCollectionVisibility()));
        result.put("isMerchant", isMerchant(user));
        return result;
    }

    public List<Map<String, Object>> getFollowers(String viewerUsername, Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User does not exist"));
        return buildUserRelationList(userFollowRepository.findByFollowingIdOrderByCreateTimeDesc(userId), true, findUserIdByUsername(viewerUsername));
    }

    public List<Map<String, Object>> getFollowing(String viewerUsername, Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User does not exist"));
        return buildUserRelationList(userFollowRepository.findByFollowerIdOrderByCreateTimeDesc(userId), false, findUserIdByUsername(viewerUsername));
    }

    public List<Map<String, Object>> getCollectedBy(String viewerUsername, Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User does not exist"));
        Long viewerId = findUserIdByUsername(viewerUsername);
        List<Long> postIds = getHomepageVisiblePosts(userId, viewerId).stream().map(DiscoverPost::getId).collect(Collectors.toList());
        if (postIds.isEmpty()) return Collections.emptyList();

        Map<Long, Integer> collectCountMap = new LinkedHashMap<>();
        Map<Long, java.util.Date> latestCollectTimeMap = new HashMap<>();
        for (DiscoverPostCollection collection : discoverPostCollectionRepository.findByPostIdInOrderByCreateTimeDesc(postIds)) {
            if (collection.getUserId() == null || collection.getUserId().equals(userId)) continue;
            collectCountMap.merge(collection.getUserId(), 1, Integer::sum);
            latestCollectTimeMap.putIfAbsent(collection.getUserId(), collection.getCreateTime());
        }
        if (collectCountMap.isEmpty()) return Collections.emptyList();

        Map<Long, User> userMap = userRepository.findAllById(collectCountMap.keySet()).stream().collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left));
        return collectCountMap.entrySet().stream().map(entry -> {
            User user = userMap.get(entry.getKey());
            if (user == null) return null;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", user.getId());
            item.put("username", defaultString(user.getUsername()));
            item.put("nickname", displayName(user));
            item.put("avatar", avatarOf(user));
            item.put("bio", defaultString(user.getBio()));
            item.put("role", defaultString(user.getRole()));
            item.put("collectCount", entry.getValue());
            item.put("latestCollectTime", formatDateTime(latestCollectTimeMap.get(entry.getKey())));
            item.put("isFollowing", viewerId != null && !viewerId.equals(user.getId()) && userFollowRepository.existsByFollowerIdAndFollowingId(viewerId, user.getId()));
            return item;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    @Transactional
    public Map<String, Object> followUser(String username, Long targetUserId) {
        User follower = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User does not exist"));
        User following = userRepository.findById(targetUserId).orElseThrow(() -> new RuntimeException("User does not exist"));
        if (follower.getId().equals(following.getId())) throw new RuntimeException("You cannot follow yourself");
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
        User follower = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User does not exist"));
        User following = userRepository.findById(targetUserId).orElseThrow(() -> new RuntimeException("User does not exist"));
        if (follower.getId().equals(following.getId())) throw new RuntimeException("You cannot unfollow yourself");
        userFollowRepository.deleteByFollowerIdAndFollowingId(follower.getId(), following.getId());
        refreshFollowCounts(follower);
        refreshFollowCounts(following);
        return buildFollowState(follower.getId(), following.getId());
    }

    public Map<String, Object> getFollowStatus(String username, Long targetUserId) {
        User follower = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User does not exist"));
        userRepository.findById(targetUserId).orElseThrow(() -> new RuntimeException("User does not exist"));
        return buildFollowState(follower.getId(), targetUserId);
    }

    public Map<String, Object> getVisitorRecords(String username) {
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User does not exist"));
        List<UserProfileVisit> viewedRecords = userProfileVisitRepository.findByViewerIdOrderByUpdateTimeDesc(currentUser.getId());
        List<UserProfileVisit> receivedRecords = userProfileVisitRepository.findByProfileUserIdOrderByUpdateTimeDesc(currentUser.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("viewedUsers", buildVisitorItems(viewedRecords, true));
        result.put("receivedVisitors", buildVisitorItems(receivedRecords, false));
        result.put("viewedCount", viewedRecords.size());
        result.put("receivedCount", receivedRecords.size());
        return result;
    }

    private Map<String, Object> buildBadge(String code, String name, String description, long current, long target, String type) {
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
        item.put("visibility", normalizeVisibility(post.getVisibility()));
        item.put("time", formatDate(post.getCreateTime()));
        Map<String, Object> authorMap = new LinkedHashMap<>();
        authorMap.put("id", author == null ? null : author.getId());
        authorMap.put("name", displayName(author));
        authorMap.put("avatar", avatarOf(author));
        authorMap.put("location", buildLocation(author));
        item.put("author", authorMap);
        return item;
    }

    private List<Map<String, Object>> buildHomepageCollections(Long userId, Long viewerUserId, boolean collectionsVisible) {
        if (!collectionsVisible) return Collections.emptyList();
        List<DiscoverPostCollection> collections = discoverPostCollectionRepository.findByUserIdOrderByCreateTimeDesc(userId);
        if (collections.isEmpty()) return Collections.emptyList();

        List<Long> postIds = collections.stream().map(DiscoverPostCollection::getPostId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, DiscoverPost> postMap = discoverPostRepository.findAllById(postIds).stream()
                .filter(this::isHomepagePostApproved)
                .filter(post -> isPostVisibleToViewer(post, viewerUserId))
                .collect(Collectors.toMap(DiscoverPost::getId, item -> item, (left, right) -> left));
        if (postMap.isEmpty()) return Collections.emptyList();

        Set<Long> authorIds = postMap.values().stream().map(DiscoverPost::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> authorMap = userRepository.findAllById(authorIds).stream().collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left));
        return collections.stream().map(collection -> {
            DiscoverPost post = postMap.get(collection.getPostId());
            if (post == null) return null;
            User author = authorMap.get(post.getUserId());
            List<String> images = readStringArray(post.getImages());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", collection.getId());
            item.put("postId", post.getId());
            item.put("title", defaultString(post.getTitle()));
            item.put("description", defaultString(post.getContent()));
            item.put("coverImage", images.isEmpty() ? "" : images.get(0));
            item.put("images", images);
            item.put("collectCount", safeInt(post.getCollectCount()));
            item.put("commentCount", safeInt(post.getCommentCount()));
            item.put("createTime", formatDate(collection.getCreateTime()));
            item.put("authorId", author == null ? null : author.getId());
            item.put("authorName", displayName(author));
            item.put("authorAvatar", avatarOf(author));
            item.put("visibility", normalizeVisibility(post.getVisibility()));
            return item;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    private List<DiscoverPost> getHomepageVisiblePosts(Long userId, Long viewerUserId) {
        return discoverPostRepository.findByUserIdAndStatusOrderByCreateTimeDesc(userId, 1).stream()
                .filter(this::isHomepagePostApproved)
                .filter(post -> isPostVisibleToViewer(post, viewerUserId))
                .collect(Collectors.toList());
    }

    private long getCollectedCount(Long userId, Long viewerUserId) {
        List<Long> postIds = getHomepageVisiblePosts(userId, viewerUserId).stream().map(DiscoverPost::getId).collect(Collectors.toList());
        if (postIds.isEmpty()) return 0L;
        return discoverPostCollectionRepository.findByPostIdInOrderByCreateTimeDesc(postIds).stream()
                .filter(item -> item.getUserId() != null && !item.getUserId().equals(userId))
                .count();
    }

    private List<Map<String, Object>> buildUserRelationList(List<UserFollow> relations, boolean followerList, Long viewerId) {
        if (relations == null || relations.isEmpty()) return Collections.emptyList();
        List<Long> userIds = relations.stream().map(item -> followerList ? item.getFollowerId() : item.getFollowingId()).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left));
        return relations.stream().map(relation -> {
            Long targetId = followerList ? relation.getFollowerId() : relation.getFollowingId();
            User targetUser = userMap.get(targetId);
            if (targetUser == null) return null;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", targetUser.getId());
            item.put("username", defaultString(targetUser.getUsername()));
            item.put("nickname", displayName(targetUser));
            item.put("avatar", avatarOf(targetUser));
            item.put("bio", defaultString(targetUser.getBio()));
            item.put("role", defaultString(targetUser.getRole()));
            item.put("followTime", formatDateTime(relation.getCreateTime()));
            item.put("isFollowing", viewerId != null && !viewerId.equals(targetUser.getId()) && userFollowRepository.existsByFollowerIdAndFollowingId(viewerId, targetUser.getId()));
            return item;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<String> readStringArray(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    private String displayName(User user) {
        if (user == null) return "Unknown";
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : defaultString(user.getUsername());
    }

    private String avatarOf(User user) {
        if (user == null || !StringUtils.hasText(user.getAvatar())) return "/default-avatar.svg";
        return user.getAvatar();
    }

    private String resolveHomepageCover(User user) {
        if (user == null) return "";
        if (StringUtils.hasText(user.getCoverPhoto())) return user.getCoverPhoto();
        return defaultString(user.getShopCover());
    }

    private String buildLocation(User user) {
        if (user == null) return "";
        return StringUtils.hasText(user.getLocation()) ? user.getLocation() : defaultString(user.getCountry());
    }

    private boolean isMerchant(User user) {
        if (user == null) return false;
        if (Integer.valueOf(1).equals(user.getIsMerchant())) return true;
        String shopStatus = defaultString(user.getShopStatus());
        if ("approved".equalsIgnoreCase(shopStatus) || "active".equalsIgnoreCase(shopStatus)) return true;
        String role = defaultString(user.getRole());
        return "merchant".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
    }

    private boolean isFollowing(String viewerUsername, Long targetUserId) {
        if (!StringUtils.hasText(viewerUsername) || targetUserId == null) return false;
        User viewer = userRepository.findByUsername(viewerUsername).orElse(null);
        if (viewer == null || viewer.getId().equals(targetUserId)) return false;
        return userFollowRepository.existsByFollowerIdAndFollowingId(viewer.getId(), targetUserId);
    }

    private Long findUserIdByUsername(String username) {
        if (!StringUtils.hasText(username)) return null;
        return userRepository.findByUsername(username).map(User::getId).orElse(null);
    }

    private boolean isHomepagePostApproved(DiscoverPost post) {
        return post != null && post.getStatus() != null && post.getStatus() == 1 && "passed".equalsIgnoreCase(defaultString(post.getAuditStatus()));
    }

    private boolean isPostVisibleToViewer(DiscoverPost post, Long viewerUserId) {
        if (post == null) return false;
        if (viewerUserId != null && viewerUserId.equals(post.getUserId())) return true;
        return !"private".equalsIgnoreCase(defaultString(post.getVisibility()));
    }

    private String normalizeVisibility(String visibility) {
        return "private".equalsIgnoreCase(defaultString(visibility)) ? "private" : "public";
    }

    private void recordProfileVisit(String viewerUsername, User profileUser) {
        if (!StringUtils.hasText(viewerUsername) || profileUser == null) return;
        User viewer = userRepository.findByUsername(viewerUsername).orElse(null);
        if (viewer == null || viewer.getId().equals(profileUser.getId())) return;
        UserProfileVisit visit = userProfileVisitRepository.findByViewerIdAndProfileUserId(viewer.getId(), profileUser.getId()).orElseGet(UserProfileVisit::new);
        visit.setViewerId(viewer.getId());
        visit.setProfileUserId(profileUser.getId());
        visit.setUpdateTime(new java.util.Date());
        userProfileVisitRepository.save(visit);
    }

    private List<Map<String, Object>> buildVisitorItems(List<UserProfileVisit> records, boolean viewedMode) {
        if (records == null || records.isEmpty()) return Collections.emptyList();
        List<Long> userIds = records.stream().map(item -> viewedMode ? item.getProfileUserId() : item.getViewerId()).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, item -> item));
        return records.stream().map(record -> {
            Long targetId = viewedMode ? record.getProfileUserId() : record.getViewerId();
            User targetUser = userMap.get(targetId);
            if (targetUser == null) return null;
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
        }).filter(Objects::nonNull).collect(Collectors.toList());
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
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private String formatDate(java.util.Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String formatDateTime(java.util.Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }
}
