package com.yayfolk.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.dto.TranslateRequest;
import com.yayfolk.backend.dto.TranslateResponse;
import com.yayfolk.backend.entity.*;
import com.yayfolk.backend.repository.*;
import com.yayfolk.backend.util.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DiscoverService {
    private static final String AUDIT_STATUS_PENDING = "pending";
    private static final String AUDIT_STATUS_PASSED = "passed";
    private static final String AUDIT_STATUS_REJECTED = "rejected";
    private static final String AUDIT_STATUS_MANUAL_REVIEW = "manual_review";
    private static final String REPORT_STATUS_PENDING = "pending";
    private static final long TRANSLATE_CACHE_DAYS = 7;
    private static final List<String> FORBIDDEN_KEYWORDS = Arrays.asList(
        "微信", "v信", "vx", "wechat", "qq", "二维码", "扫码", "联系方式", "广告", "代购", "返利", "引流", "推广"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)1\\d{10}(?!\\d)");
    private static final Pattern WECHAT_PATTERN = Pattern.compile("(?i)(微信|vx|v信|wechat|wx)\\s*[:：]?[\\s\\-_]*[a-z][a-z0-9\\-_]{5,19}");
    private static final Pattern QQ_PATTERN = Pattern.compile("(?i)(qq|q号|企鹅号)?\\s*[:：]?[\\s\\-_]*[1-9][0-9]{4,11}");
    
    private static final List<String> MODERATION_KEYWORDS = Arrays.asList(
        "微信", "v信", "vx", "wechat", "wx", "qq",
        "二维码", "扫码", "联系方式", "联系电话", "加我",
        "广告", "推广", "引流", "代购", "返利", "私聊",
        "品牌合作", "品牌广告", "招商", "代理", "加盟"
    );
    private static final Pattern MODERATION_PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:1\\d{10}|0\\d{2,3}-?\\d{7,8})(?!\\d)");
    private static final Pattern MODERATION_WECHAT_PATTERN = Pattern.compile("(?i)(微信|wechat|wx|vx|v信)\\s*[:：]?[\\s\\-_]*[a-z][a-z0-9\\-_]{5,19}");
    private static final Pattern MODERATION_QQ_PATTERN = Pattern.compile("(?i)(qq|q号|企鹅号)\\s*[:：]?[\\s\\-_]*[1-9][0-9]{4,11}");
    private static final Pattern MODERATION_URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");

    private static final Map<String, Set<String>> TAG_I18N_MAP = new HashMap<>();
    static {
        Set<String> travelTags = new HashSet<>(Arrays.asList("旅行", "travel", "travel", "여행"));
        Set<String> foodTags = new HashSet<>(Arrays.asList("美食", "food", "グルメ", "음식"));
        Set<String> sceneryTags = new HashSet<>(Arrays.asList("风景", "scenery", "風景", "풍경"));
        Set<String> guideTags = new HashSet<>(Arrays.asList("攻略", "guide", "攻略", "가이드"));
        Set<String> hotelTags = new HashSet<>(Arrays.asList("住宿", "hotel", "宿泊", "숙박"));
        Set<String> transportTags = new HashSet<>(Arrays.asList("交通", "transport", "交通", "교통"));
        Set<String> costumeTags = new HashSet<>(Arrays.asList("服饰妆造", "服饰", "妆造", "clothing and styling", "costume", "服飾と化粧", "의상과 메이크업"));
        Set<String> artistryTags = new HashSet<>(Arrays.asList("美术造物", "美术", "造物", "arts and craft", "artistry", "美術と造物", "미술 조형"));
        Set<String> folkloreTags = new HashSet<>(Arrays.asList("民俗节气", "民俗", "节气", "folk customs and solar terms", "folklore", "民俗と節気", "민속 절기"));
        Set<String> operaTags = new HashSet<>(Arrays.asList("戏曲演绎", "戏曲", "演绎", "opera performance", "opera", "戯曲演繹", "희곡 공연"));
        Set<String> textileTags = new HashSet<>(Arrays.asList("织物手工", "织物", "手工", "textile handcraft", "textile", "織物手工", "직물 공예"));
        
        TAG_I18N_MAP.put("travel", travelTags);
        TAG_I18N_MAP.put("food", foodTags);
        TAG_I18N_MAP.put("scenery", sceneryTags);
        TAG_I18N_MAP.put("guide", guideTags);
        TAG_I18N_MAP.put("hotel", hotelTags);
        TAG_I18N_MAP.put("transport", transportTags);
        TAG_I18N_MAP.put("服饰妆造", costumeTags);
        TAG_I18N_MAP.put("美术造物", artistryTags);
        TAG_I18N_MAP.put("民俗节气", folkloreTags);
        TAG_I18N_MAP.put("戏曲演绎", operaTags);
        TAG_I18N_MAP.put("织物手工", textileTags);
    }
    
    private final DiscoverPostRepository postRepository;
    private final DiscoverPostCollectionRepository collectionRepository;
    private final DiscoverPostCommentRepository commentRepository;
    private final DiscoverPostCommentLikeRepository commentLikeRepository;
    private final DiscoverPostHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PostReportRepository postReportRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final TranslateService translateService;
    private final BaiduContentAuditService baiduContentAuditService;
    private final OssUtil ossUtil;

    public DiscoverService(DiscoverPostRepository postRepository,
                           DiscoverPostCollectionRepository collectionRepository,
                           DiscoverPostCommentRepository commentRepository,
                           DiscoverPostCommentLikeRepository commentLikeRepository,
                           DiscoverPostHistoryRepository historyRepository,
                           UserRepository userRepository,
                           NotificationRepository notificationRepository,
                           PostReportRepository postReportRepository,
                           ObjectMapper objectMapper,
                           StringRedisTemplate redisTemplate,
                           TranslateService translateService,
                           BaiduContentAuditService baiduContentAuditService,
                           OssUtil ossUtil) {
        this.postRepository = postRepository;
        this.collectionRepository = collectionRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.postReportRepository = postReportRepository;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.translateService = translateService;
        this.baiduContentAuditService = baiduContentAuditService;
        this.ossUtil = ossUtil;
    }

    public List<Map<String, Object>> getFeed(String username, String category, String keyword, String sortBy) {
        Long userId = findUserId(username);
        List<DiscoverPost> allPosts = postRepository.findByStatusAndAuditStatusOrderByCreateTimeDesc(1, AUDIT_STATUS_PASSED);
        String normalizedCategory = normalizeText(category);
        String normalizedKeyword = normalizeText(keyword);
        
        Set<String> matchedTagIds = findMatchingTagIds(normalizedKeyword);

        Map<Long, User> authorMap = getAuthorMap(allPosts);

        List<DiscoverPost> filteredPosts = allPosts.stream()
            .filter(post -> {
                if (!StringUtils.hasText(normalizedCategory) || "all".equalsIgnoreCase(normalizedCategory)) {
                    return true;
                }
                return normalizedCategory.equalsIgnoreCase(defaultString(post.getCategory()));
            })
            .filter(post -> {
                if (!StringUtils.hasText(normalizedKeyword)) {
                    return true;
                }
                String title = defaultString(post.getTitle()).toLowerCase();
                String content = defaultString(post.getContent()).toLowerCase();
                String tags = defaultString(post.getTags()).toLowerCase();
                String keywordLower = normalizedKeyword.toLowerCase();
                
                if (title.contains(keywordLower) || content.contains(keywordLower) || tags.contains(keywordLower)) {
                    return true;
                }
                
                if (matchedTagIds != null && !matchedTagIds.isEmpty()) {
                    String postCategory = defaultString(post.getCategory()).toLowerCase();
                    if (matchedTagIds.contains(postCategory)) {
                        return true;
                    }
                    for (String tagId : matchedTagIds) {
                        if (tags.contains(tagId.toLowerCase())) {
                            return true;
                        }
                    }
                }
                
                User author = authorMap.get(post.getUserId());
                if (author != null) {
                    String authorName = defaultString(author.getNickname()).toLowerCase();
                    if (authorName.contains(keywordLower)) {
                        return true;
                    }
                }
                
                return false;
            })
            .collect(Collectors.toList());

        if ("hot".equals(sortBy)) {
            filteredPosts.sort((a, b) -> {
                int scoreA = safeInt(a.getViewCount()) + safeInt(a.getCollectCount()) * 5 + safeInt(a.getCommentCount()) * 3;
                int scoreB = safeInt(b.getViewCount()) + safeInt(b.getCollectCount()) * 5 + safeInt(b.getCommentCount()) * 3;
                return Integer.compare(scoreB, scoreA);
            });
        }

        Set<Long> collectedSet = getCollectedPostSet(userId, filteredPosts);

        return filteredPosts.stream()
            .map(post -> toPostSummary(post, authorMap.get(post.getUserId()), collectedSet.contains(post.getId())))
            .collect(Collectors.toList());
    }

    public Map<String, Object> getPostDetail(String username, Long postId) {
        Long userId = findUserId(username);
        DiscoverPost post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post does not exist"));

        boolean publicVisible = isApprovedPost(post);
        boolean ownerAccess = canOwnerAccessPost(post, userId);
        if (!publicVisible && !ownerAccess) {
            throw new RuntimeException("Post does not exist");
        }

        if (publicVisible) {
            recordHistory(userId, post);
            post.setViewCount(Math.max(0, safeInt(post.getViewCount())) + 1);
            postRepository.save(post);
        }

        boolean bookmarked = publicVisible && collectionRepository.existsByUserIdAndPostId(userId, postId);
        User author = userRepository.findById(post.getUserId()).orElse(null);
        Map<String, Object> detail = toPostSummary(post, author, bookmarked);

        List<DiscoverPostComment> comments = commentRepository.findByPostIdOrderByCreateTimeAsc(postId);
        Set<Long> commentUserIds = comments.stream().map(DiscoverPostComment::getUserId).collect(Collectors.toSet());
        Set<Long> replyToUserIds = comments.stream()
            .map(DiscoverPostComment::getReplyToUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        commentUserIds.addAll(replyToUserIds);

        Map<Long, User> commentUserMap = userRepository.findAllById(commentUserIds)
            .stream()
            .collect(Collectors.toMap(User::getId, item -> item));

        Set<Long> likedCommentIds = getLikedCommentIds(userId, comments);

        List<Map<String, Object>> commentList = comments.stream()
            .map(comment -> toCommentMap(comment, commentUserMap, likedCommentIds))
            .collect(Collectors.toList());

        detail.put("commentList", commentList);
        detail.put("comments", commentList.size());
        return detail;
    }

    public Map<String, Object> createPost(String username, Map<String, Object> payload) {
        User author = findUser(username);
        Long userId = author.getId();
        String title = normalizeText((String) payload.get("title"));
        String content = normalizeText((String) payload.get("content"));
        if (!StringUtils.hasText(title)) {
            throw new RuntimeException("Please enter a title");
        }
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("Please enter content");
        }

        String category = normalizeText((String) payload.get("category"));
        if (!StringUtils.hasText(category)) {
            category = "travel";
        }
        List<String> imageUrls = readStringArray(writeJsonArray(payload.get("images")));
        List<String> videoUrls = readStringArray(writeJsonArray(payload.get("videos")));
        ModerationDecision decision = runModerationPipeline(author, null, title, content, imageUrls, videoUrls);

        DiscoverPost post = new DiscoverPost();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setSourceLang(detectLanguage(content));
        post.setCategory(category);
        post.setImages(writeJsonArray(imageUrls));
        post.setTags(writeJsonArray(payload.get("tags")));
        post.setStatus(1);
        post.setAuditStatus(decision.getAuditStatus());
        post.setAuditRemark(decision.getAuditRemark());

        DiscoverPost saved = postRepository.save(post);
        return toPostSummary(saved, author, false);
    }

    @Transactional
    public Map<String, Object> updatePost(String username, Long postId, Map<String, Object> payload) {
        User author = findUser(username);
        Long userId = author.getId();
        DiscoverPost post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post does not exist"));
        if (!userId.equals(post.getUserId())) {
            throw new RuntimeException("You do not have permission to edit this post");
        }
        if (!canOwnerEditPost(post, userId)) {
            throw new RuntimeException("Post does not exist");
        }

        String title = normalizeText((String) payload.get("title"));
        String content = normalizeText((String) payload.get("content"));
        if (!StringUtils.hasText(title)) {
            throw new RuntimeException("Please enter a title");
        }
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("Please enter content");
        }

        String category = normalizeText((String) payload.get("category"));
        if (!StringUtils.hasText(category)) {
            category = "travel";
        }

        List<String> oldImages = readStringArray(post.getImages());
        List<String> newImages = readStringArray(writeJsonArray(payload.get("images")));
        List<String> videoUrls = readStringArray(writeJsonArray(payload.get("videos")));
        for (String oldImage : oldImages) {
            if (!newImages.contains(oldImage) && ossUtil.isOssUrl(oldImage)) {
                ossUtil.deleteFile(oldImage);
                System.out.println("Deleted removed post image: " + oldImage);
            }
        }
        ModerationDecision decision = runModerationPipeline(author, postId, title, content, newImages, videoUrls);

        post.setTitle(title);
        post.setContent(content);
        post.setSourceLang(detectLanguage(content));
        post.setCategory(category);
        post.setImages(writeJsonArray(newImages));
        post.setTags(writeJsonArray(payload.get("tags")));
        post.setStatus(1);
        post.setAuditStatus(decision.getAuditStatus());
        post.setAuditRemark(decision.getAuditRemark());
        DiscoverPost saved = postRepository.save(post);
        clearPostTranslateCache(postId);

        return toPostSummary(saved, author, false);
    }

    @Transactional
    public Map<String, Object> toggleCollection(String username, Long postId) {
        Long userId = findUserId(username);
        DiscoverPost post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post does not exist"));
        if (!isApprovedPost(post)) {
            throw new RuntimeException("Post does not exist");
        }

        boolean collected;
        if (collectionRepository.existsByUserIdAndPostId(userId, postId)) {
            collectionRepository.deleteByUserIdAndPostId(userId, postId);
            post.setCollectCount(Math.max(0, safeInt(post.getCollectCount()) - 1));
            collected = false;
        } else {
            DiscoverPostCollection collection = new DiscoverPostCollection();
            collection.setPostId(postId);
            collection.setUserId(userId);
            collectionRepository.save(collection);
            post.setCollectCount(safeInt(post.getCollectCount()) + 1);
            collected = true;

            if (!userId.equals(post.getUserId())) {
                User collector = userRepository.findById(userId).orElse(null);
                String collectorName = collector != null ? displayName(collector) : "Someone";
                Notification notification = new Notification();
                notification.setUserId(post.getUserId());
                notification.setType("collection");
                notification.setFromUserId(userId);
                notification.setPostId(postId);
                notification.setContent(collectorName + " bookmarked your post");
                notification.setIsRead(false);
                notificationRepository.save(notification);
            }
        }
        postRepository.save(post);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("bookmarked", collected);
        result.put("collects", safeInt(post.getCollectCount()));
        return result;
    }

    public Map<String, Object> addComment(String username, Long postId, Map<String, String> payload) {
        Long userId = findUserId(username);
        DiscoverPost post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post does not exist"));
        if (!isApprovedPost(post)) {
            throw new RuntimeException("Post does not exist");
        }

        String content = normalizeText(payload.get("content"));
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("Comment content cannot be empty");
        }

        DiscoverPostComment comment = new DiscoverPostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setSourceLang(detectLanguage(content));

        String parentIdStr = payload.get("parentId");
        if (StringUtils.hasText(parentIdStr)) {
            Long parentId = Long.parseLong(parentIdStr);
            DiscoverPostComment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Reply target does not exist"));
            if (!parentComment.getPostId().equals(postId)) {
                throw new RuntimeException("Reply target does not belong to this post");
            }
            comment.setParentId(parentId);
            comment.setReplyToUserId(parentComment.getUserId());
        }

        DiscoverPostComment savedComment = commentRepository.save(comment);
        post.setCommentCount(safeInt(post.getCommentCount()) + 1);
        postRepository.save(post);

        User user = userRepository.findById(userId).orElse(null);
        String commenterName = user != null ? displayName(user) : "Someone";

        if (comment.getReplyToUserId() != null && !userId.equals(comment.getReplyToUserId())) {
            Notification replyNotification = new Notification();
            replyNotification.setUserId(comment.getReplyToUserId());
            replyNotification.setType("comment");
            replyNotification.setFromUserId(userId);
            replyNotification.setPostId(postId);
            replyNotification.setCommentId(savedComment.getId());
            replyNotification.setContent(commenterName + " replied to your comment: " + truncateContent(content, 50));
            replyNotification.setIsRead(false);
            notificationRepository.save(replyNotification);
        } else if (!userId.equals(post.getUserId())) {
            Notification commentNotification = new Notification();
            commentNotification.setUserId(post.getUserId());
            commentNotification.setType("comment");
            commentNotification.setFromUserId(userId);
            commentNotification.setPostId(postId);
            commentNotification.setCommentId(savedComment.getId());
            commentNotification.setContent(commenterName + " commented on your post: " + truncateContent(content, 50));
            commentNotification.setIsRead(false);
            notificationRepository.save(commentNotification);
        }

        Map<Long, User> userMap = new HashMap<>();
        userMap.put(userId, user);
        if (savedComment.getReplyToUserId() != null) {
            User replyToUser = userRepository.findById(savedComment.getReplyToUserId()).orElse(null);
            userMap.put(savedComment.getReplyToUserId(), replyToUser);
        }

        return toCommentMap(savedComment, userMap, Collections.emptySet());
    }

    @Transactional
    public Map<String, Object> toggleCommentLike(String username, Long commentId) {
        Long userId = findUserId(username);
        DiscoverPostComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("评论不存在"));

        boolean liked;
        if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
            comment.setLikeCount(Math.max(0, safeInt(comment.getLikeCount()) - 1));
            liked = false;
        } else {
            DiscoverPostCommentLike like = new DiscoverPostCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeRepository.save(like);
            comment.setLikeCount(safeInt(comment.getLikeCount()) + 1);
            liked = true;
        }
        commentRepository.save(comment);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("liked", liked);
        result.put("likes", safeInt(comment.getLikeCount()));
        return result;
    }

    @Transactional
    public void deleteComment(String username, Long commentId) {
        Long userId = findUserId(username);
        DiscoverPostComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("评论不存在"));

        DiscoverPost post = postRepository.findById(comment.getPostId())
            .orElseThrow(() -> new RuntimeException("帖子不存在"));

        if (!userId.equals(comment.getUserId()) && !userId.equals(post.getUserId())) {
            throw new RuntimeException("无权限删除该评论");
        }

        List<DiscoverPostComment> postComments = commentRepository.findByPostIdOrderByCreateTimeAsc(post.getId());
        Set<Long> idsToDelete = collectDescendantCommentIds(commentId, postComments);
        idsToDelete.add(commentId);
        List<Long> commentIds = new ArrayList<Long>(idsToDelete);

        clearCommentTranslateCache(commentIds);
        commentRepository.deleteById(commentId);

        post.setCommentCount(Math.max(0, safeInt(post.getCommentCount()) - commentIds.size()));
        postRepository.save(post);
    }

    private Set<Long> collectDescendantCommentIds(Long rootCommentId, List<DiscoverPostComment> comments) {
        Map<Long, List<Long>> childrenMap = new HashMap<Long, List<Long>>();
        for (DiscoverPostComment item : comments) {
            if (item.getParentId() != null) {
                if (!childrenMap.containsKey(item.getParentId())) {
                    childrenMap.put(item.getParentId(), new ArrayList<Long>());
                }
                childrenMap.get(item.getParentId()).add(item.getId());
            }
        }

        Set<Long> result = new HashSet<Long>();
        Deque<Long> stack = new ArrayDeque<Long>();
        stack.push(rootCommentId);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            List<Long> children = childrenMap.get(current);
            if (children == null || children.isEmpty()) {
                continue;
            }
            for (Long childId : children) {
                if (result.add(childId)) {
                    stack.push(childId);
                }
            }
        }
        return result;
    }

    public List<Map<String, Object>> getMyPosts(String username) {
        Long userId = findUserId(username);
        List<DiscoverPost> posts = postRepository.findByUserId(userId)
            .stream()
            .filter(this::shouldShowInMyPosts)
            .sorted((a, b) -> {
                Date at = a.getCreateTime();
                Date bt = b.getCreateTime();
                if (at == null && bt == null) {
                    return 0;
                }
                if (at == null) {
                    return 1;
                }
                if (bt == null) {
                    return -1;
                }
                return bt.compareTo(at);
            })
            .collect(Collectors.toList());
        User author = userRepository.findById(userId).orElse(null);
        Set<Long> collectedSet = getCollectedPostSet(userId, posts);
        return posts.stream()
            .map(post -> toPostSummary(post, author, collectedSet.contains(post.getId())))
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMyPost(String username, Long postId) {
        Long userId = findUserId(username);
        DiscoverPost post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("帖子不存在"));
        if (!userId.equals(post.getUserId())) {
            throw new RuntimeException("无权限删除该帖子");
        }

        List<String> images = readStringArray(post.getImages());
        for (String image : images) {
            if (ossUtil.isOssUrl(image)) {
                ossUtil.deleteFile(image);
                System.out.println("删除帖子图片：" + image);
            }
        }

        post.setStatus(0);
        post.setAuditStatus("deleted");
        post.setAuditRemark(null);
        postRepository.save(post);
        clearPostTranslateCache(postId);
        collectionRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId);
        historyRepository.deleteByPostId(postId);
    }

    public List<Map<String, Object>> getMyCollections(String username) {
        Long userId = findUserId(username);
        List<DiscoverPostCollection> collections = collectionRepository.findByUserIdOrderByCreateTimeDesc(userId);
        if (collections.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> postIds = collections.stream().map(DiscoverPostCollection::getPostId).collect(Collectors.toList());
        Map<Long, DiscoverPost> postMap = postRepository.findAllById(postIds).stream()
            .filter(this::isApprovedPost)
            .collect(Collectors.toMap(DiscoverPost::getId, item -> item));
        Map<Long, User> authorMap = getAuthorMap(new ArrayList<DiscoverPost>(postMap.values()));

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DiscoverPostCollection collection : collections) {
            DiscoverPost post = postMap.get(collection.getPostId());
            if (post == null) {
                continue;
            }
            Map<String, Object> item = toPostSummary(post, authorMap.get(post.getUserId()), true);
            item.put("collectedAt", formatDate(collection.getCreateTime()));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void removeMyCollection(String username, Long postId) {
        Long userId = findUserId(username);
        collectionRepository.deleteByUserIdAndPostId(userId, postId);
    }

    @Transactional
    public Map<String, Object> reportPost(String username, Long postId, Map<String, Object> payload) {
        User reporter = findUser(username);
        DiscoverPost post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post does not exist"));
        if (!isActivePost(post)) {
            throw new RuntimeException("Post does not exist");
        }
        if (reporter.getId() != null && reporter.getId().equals(post.getUserId())) {
            throw new RuntimeException("You cannot report your own post");
        }

        Object reasonObj = payload == null ? null : payload.get("reason");
        String reason = normalizeText(reasonObj == null ? "" : String.valueOf(reasonObj));
        if (!StringUtils.hasText(reason)) {
            reason = "Reported as potentially violating community policy";
        }

        try {
            if (postReportRepository.existsByPostIdAndReporterIdAndStatus(postId, reporter.getId(), REPORT_STATUS_PENDING)) {
                throw new RuntimeException("You have already reported this post");
            }

            PostReport report = new PostReport();
            report.setPostId(postId);
            report.setReporterId(reporter.getId());
            report.setReason(reason);
            report.setStatus(REPORT_STATUS_PENDING);
            postReportRepository.save(report);

            long pendingReportCount = postReportRepository.countByPostIdAndStatus(postId, REPORT_STATUS_PENDING);
            if (!AUDIT_STATUS_REJECTED.equalsIgnoreCase(defaultString(post.getAuditStatus()))) {
                post.setAuditStatus(AUDIT_STATUS_MANUAL_REVIEW);
                post.setAuditRemark("User reported (" + pendingReportCount + "): " + truncateContent(reason, 120));
                postRepository.save(post);
            }

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("postId", postId);
            result.put("auditStatus", post.getAuditStatus());
            result.put("auditRemark", post.getAuditRemark());
            result.put("pendingReportCount", pendingReportCount);
            return result;
        } catch (DataAccessException e) {
            log.error("Failed to report post, postId={}", postId, e);
            throw new RuntimeException("Report service temporarily unavailable, please try again later");
        }
    }

    public List<Map<String, Object>> getMyHistory(String username) {
        Long userId = findUserId(username);
        List<DiscoverPostHistory> histories = historyRepository.findByUserIdOrderByLastViewTimeDesc(userId);
        if (histories.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> postIds = histories.stream().map(DiscoverPostHistory::getPostId).collect(Collectors.toList());
        Map<Long, DiscoverPost> postMap = postRepository.findAllById(postIds).stream()
            .filter(this::isApprovedPost)
            .collect(Collectors.toMap(DiscoverPost::getId, item -> item));
        Map<Long, User> authorMap = getAuthorMap(new ArrayList<DiscoverPost>(postMap.values()));
        Set<Long> collectedSet = getCollectedSetByPostIds(userId, postIds);

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DiscoverPostHistory history : histories) {
            DiscoverPost post = postMap.get(history.getPostId());
            if (post == null) {
                continue;
            }
            Map<String, Object> item = toPostSummary(post, authorMap.get(post.getUserId()), collectedSet.contains(post.getId()));
            item.put("viewedAt", formatDate(history.getLastViewTime()));
            item.put("viewCount", safeInt(history.getViewCount()));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void clearMyHistory(String username) {
        Long userId = findUserId(username);
        historyRepository.deleteByUserId(userId);
    }

    public Map<String, Object> getMyStats(String username) {
        Long userId = findUserId(username);
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("posts", postRepository.countByUserIdAndStatus(userId, 1));
        stats.put("collections", collectionRepository.countByUserId(userId));
        stats.put("history", historyRepository.countByUserId(userId));
        return stats;
    }

    private void recordHistory(Long userId, DiscoverPost post) {
        Optional<DiscoverPostHistory> optional = historyRepository.findByUserIdAndPostId(userId, post.getId());
        DiscoverPostHistory history;
        if (optional.isPresent()) {
            history = optional.get();
            history.setViewCount(safeInt(history.getViewCount()) + 1);
            history.setLastViewTime(new Date());
        } else {
            history = new DiscoverPostHistory();
            history.setUserId(userId);
            history.setPostId(post.getId());
            history.setViewCount(1);
            history.setLastViewTime(new Date());
        }
        historyRepository.save(history);
    }

    private Set<Long> getCollectedPostSet(Long userId, List<DiscoverPost> posts) {
        if (posts.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> postIds = posts.stream().map(DiscoverPost::getId).collect(Collectors.toList());
        return getCollectedSetByPostIds(userId, postIds);
    }

    private Set<Long> getCollectedSetByPostIds(Long userId, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        return collectionRepository.findByUserIdAndPostIdIn(userId, postIds)
            .stream()
            .map(DiscoverPostCollection::getPostId)
            .collect(Collectors.toSet());
    }

    private Map<Long, User> getAuthorMap(List<DiscoverPost> posts) {
        Set<Long> userIds = posts.stream().map(DiscoverPost::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(userIds)
            .stream()
            .collect(Collectors.toMap(User::getId, item -> item));
    }

    public Map<String, Object> getSharePostDetail(Long postId) {
        DiscoverPost post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post does not exist"));
        if (!isApprovedPost(post)) {
            throw new RuntimeException("Post does not exist");
        }

        post.setViewCount(Math.max(0, safeInt(post.getViewCount())) + 1);
        postRepository.save(post);

        User author = userRepository.findById(post.getUserId()).orElse(null);
        return toPostSummary(post, author, false);
    }

    public Map<String, Object> translatePost(String username, Long postId, String targetLang) {
        Long userId = findUserId(username);
        DiscoverPost post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post does not exist"));
        if (!publiclyVisibleOrOwnerAccessible(post, userId)) {
            throw new RuntimeException("Post does not exist");
        }
        String sourceLang = normalizeSourceLang(post.getSourceLang(), post.getContent());
        return translateWithCache(buildPostCacheKey(postId, targetLang), post.getContent(), sourceLang, targetLang);
    }

    public Map<String, Object> translateComment(String username, Long commentId, String targetLang) {
        findUserId(username);
        DiscoverPostComment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("评论不存在"));
        String sourceLang = normalizeSourceLang(comment.getSourceLang(), comment.getContent());
        return translateWithCache(buildCommentCacheKey(commentId, targetLang), comment.getContent(), sourceLang, targetLang);
    }

    private Map<String, Object> toPostSummary(DiscoverPost post, User author, boolean bookmarked) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("id", post.getId());
        item.put("title", defaultString(post.getTitle()));
        item.put("content", defaultString(post.getContent()));
        item.put("sourceLang", normalizeSourceLang(post.getSourceLang(), post.getContent()));
        item.put("category", defaultString(post.getCategory()));
        item.put("images", readStringArray(post.getImages()));
        item.put("hashtags", readStringArray(post.getTags()));
        item.put("collects", safeInt(post.getCollectCount()));
        item.put("comments", safeInt(post.getCommentCount()));
        item.put("viewCount", safeInt(post.getViewCount()));
        item.put("time", formatDate(post.getCreateTime()));
        item.put("bookmarked", bookmarked);
        item.put("auditStatus", defaultString(post.getAuditStatus()));
        item.put("auditRemark", defaultString(post.getAuditRemark()));

        Map<String, Object> authorMap = new HashMap<String, Object>();
        authorMap.put("id", author == null ? null : author.getId());
        authorMap.put("name", displayName(author));
        authorMap.put("avatar", avatarOf(author));
        authorMap.put("location", author == null ? "" : defaultString(author.getCountry()));
        authorMap.put("following", false);
        item.put("author", authorMap);
        return item;
    }

    private boolean isActivePost(DiscoverPost post) {
        return post != null && post.getStatus() != null && post.getStatus() == 1;
    }

    private boolean isRejectedPost(DiscoverPost post) {
        return post != null && AUDIT_STATUS_REJECTED.equalsIgnoreCase(defaultString(post.getAuditStatus()));
    }

    private boolean isApprovedPost(DiscoverPost post) {
        return isActivePost(post) && AUDIT_STATUS_PASSED.equalsIgnoreCase(defaultString(post.getAuditStatus()));
    }

    private boolean isOwnerPost(DiscoverPost post, Long userId) {
        return post != null && userId != null && userId.equals(post.getUserId());
    }

    private boolean canOwnerAccessPost(DiscoverPost post, Long userId) {
        return isOwnerPost(post, userId) && (isActivePost(post) || isRejectedPost(post));
    }

    private boolean canOwnerEditPost(DiscoverPost post, Long userId) {
        return canOwnerAccessPost(post, userId);
    }

    private boolean publiclyVisibleOrOwnerAccessible(DiscoverPost post, Long userId) {
        return isApprovedPost(post) || canOwnerAccessPost(post, userId);
    }

    private boolean shouldShowInMyPosts(DiscoverPost post) {
        return isActivePost(post) || isRejectedPost(post);
    }

    private Long findUserId(String username) {
        return findUser(username).getId();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    private ModerationDecision runModerationPipeline(User author,
                                                     Long editingPostId,
                                                     String title,
                                                     String content,
                                                     List<String> imageUrls,
                                                     List<String> videoUrls) {
        String ruleReason = detectRuleViolation(author, editingPostId, title, content);
        if (StringUtils.hasText(ruleReason)) {
            return new ModerationDecision(AUDIT_STATUS_REJECTED, "Rule blocked: " + ruleReason);
        }

        BaiduContentAuditService.AuditOutcome aiResult = baiduContentAuditService.auditPostContent(
            buildTextForAudit(title, content),
            imageUrls,
            videoUrls
        );

        if (aiResult.getDecision() == BaiduContentAuditService.Decision.REJECT) {
            return new ModerationDecision(AUDIT_STATUS_REJECTED, "AI blocked: " + aiResult.getReason());
        }
        if (aiResult.getDecision() == BaiduContentAuditService.Decision.REVIEW) {
            return new ModerationDecision(AUDIT_STATUS_MANUAL_REVIEW, "AI uncertain: " + aiResult.getReason());
        }
        return new ModerationDecision(AUDIT_STATUS_PASSED, null);
    }

    private String detectRuleViolation(User author, Long editingPostId, String title, String content) {
        if (author == null || author.getId() == null) {
            return "Invalid author";
        }
        if (author.getStatus() != null && author.getStatus() == 0) {
            return "Account is disabled";
        }

        String fullText = buildTextForAudit(title, content);
        if (containsForbiddenContent(fullText)) {
            return "Contains prohibited contact, advertising, or banned keywords";
        }
        if (isDuplicatePost(author.getId(), editingPostId, title, content)) {
            return "Duplicate content detected";
        }
        return null;
    }

    private boolean containsForbiddenContent(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String keyword : MODERATION_KEYWORDS) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return MODERATION_PHONE_PATTERN.matcher(text).find()
            || MODERATION_WECHAT_PATTERN.matcher(text).find()
            || MODERATION_QQ_PATTERN.matcher(text).find()
            || MODERATION_URL_PATTERN.matcher(text).find();
    }

    private boolean isDuplicatePost(Long userId, Long editingPostId, String title, String content) {
        String normalizedNew = normalizeForDuplicate(buildTextForAudit(title, content));
        if (!StringUtils.hasText(normalizedNew) || normalizedNew.length() < 24) {
            return false;
        }

        List<DiscoverPost> historyPosts = postRepository.findByUserIdAndStatusOrderByCreateTimeDesc(userId, 1);
        for (DiscoverPost existing : historyPosts) {
            if (existing == null || existing.getId() == null) {
                continue;
            }
            if (editingPostId != null && editingPostId.equals(existing.getId())) {
                continue;
            }
            String normalizedExisting = normalizeForDuplicate(
                buildTextForAudit(defaultString(existing.getTitle()), defaultString(existing.getContent()))
            );
            if (normalizedNew.equals(normalizedExisting)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeForDuplicate(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.toLowerCase().replaceAll("[\\p{Punct}\\p{IsPunctuation}\\s]+", "");
    }

    private String buildTextForAudit(String title, String content) {
        String safeTitle = defaultString(title);
        String safeContent = defaultString(content);
        if (!StringUtils.hasText(safeTitle)) {
            return safeContent;
        }
        if (!StringUtils.hasText(safeContent)) {
            return safeTitle;
        }
        return safeTitle + "\n" + safeContent;
    }

    private static class ModerationDecision {
        private final String auditStatus;
        private final String auditRemark;

        private ModerationDecision(String auditStatus, String auditRemark) {
            this.auditStatus = auditStatus;
            this.auditRemark = auditRemark;
        }

        public String getAuditStatus() {
            return auditStatus;
        }

        public String getAuditRemark() {
            return auditRemark;
        }
    }

    private List<String> readStringArray(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<String>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    private String writeJsonArray(Object value) {
        try {
            List<String> result = new ArrayList<String>();
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    if (item != null) {
                        String text = normalizeText(String.valueOf(item));
                        if (StringUtils.hasText(text)) {
                            result.add(text);
                        }
                    }
                }
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String displayName(User user) {
        if (user == null) {
            return "未知用户";
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

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Set<String> findMatchingTagIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        Set<String> matchedIds = new HashSet<>();
        String keywordLower = keyword.toLowerCase();
        for (Map.Entry<String, Set<String>> entry : TAG_I18N_MAP.entrySet()) {
            for (String tagValue : entry.getValue()) {
                if (tagValue.toLowerCase().contains(keywordLower) || keywordLower.contains(tagValue.toLowerCase())) {
                    matchedIds.add(entry.getKey());
                    break;
                }
            }
        }
        return matchedIds.isEmpty() ? null : matchedIds;
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private Set<Long> getLikedCommentIds(Long userId, List<DiscoverPostComment> comments) {
        if (comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> commentIds = comments.stream().map(DiscoverPostComment::getId).collect(Collectors.toList());
        return commentLikeRepository.findByUserIdAndCommentIdIn(userId, commentIds)
            .stream()
            .map(DiscoverPostCommentLike::getCommentId)
            .collect(Collectors.toSet());
    }

    private Map<String, Object> toCommentMap(DiscoverPostComment comment, Map<Long, User> userMap, Set<Long> likedCommentIds) {
        User commentUser = userMap.get(comment.getUserId());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", comment.getId());
        map.put("userId", comment.getUserId());
        map.put("author", displayName(commentUser));
        map.put("avatar", avatarOf(commentUser));
        map.put("content", comment.getContent());
        map.put("sourceLang", normalizeSourceLang(comment.getSourceLang(), comment.getContent()));
        map.put("time", formatDate(comment.getCreateTime()));
        map.put("likes", safeInt(comment.getLikeCount()));
        map.put("liked", likedCommentIds.contains(comment.getId()));

        if (comment.getParentId() != null) {
            map.put("parentId", comment.getParentId());
            User replyToUser = userMap.get(comment.getReplyToUserId());
            map.put("replyTo", displayName(replyToUser));
        }

        return map;
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    private Map<String, Object> translateWithCache(String cacheKey, String originalText, String sourceLang, String targetLang) {
        String normalizedTarget = normalizeTargetLang(targetLang);
        String normalizedSource = normalizeSourceLang(sourceLang, originalText);
        if (!StringUtils.hasText(originalText)) {
            throw new RuntimeException("原文不能为空");
        }

        if (normalizedSource.equalsIgnoreCase(normalizedTarget)) {
            Map<String, Object> sameLangResult = new HashMap<String, Object>();
            sameLangResult.put("translatedText", originalText);
            sameLangResult.put("sourceLang", normalizedSource);
            sameLangResult.put("targetLang", normalizedTarget);
            sameLangResult.put("fromCache", true);
            return sameLangResult;
        }

        String cachedText = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cachedText)) {
            Map<String, Object> cachedResult = new HashMap<String, Object>();
            cachedResult.put("translatedText", cachedText);
            cachedResult.put("sourceLang", normalizedSource);
            cachedResult.put("targetLang", normalizedTarget);
            cachedResult.put("fromCache", true);
            return cachedResult;
        }

        TranslateRequest request = new TranslateRequest();
        request.setText(originalText);
        request.setSourceLang(normalizedSource);
        request.setTargetLang(normalizedTarget);
        TranslateResponse response = translateService.translate(request);
        if (response == null || response.getCode() != 200 || !StringUtils.hasText(response.getTranslatedText())) {
            String errorMessage = response != null && StringUtils.hasText(response.getMessage()) ? response.getMessage() : "Translation failed, please try again";
            throw new RuntimeException(errorMessage);
        }

        redisTemplate.opsForValue().set(cacheKey, response.getTranslatedText(), TRANSLATE_CACHE_DAYS, TimeUnit.DAYS);
        Map<String, Object> translatedResult = new HashMap<String, Object>();
        translatedResult.put("translatedText", response.getTranslatedText());
        translatedResult.put("sourceLang", normalizedSource);
        translatedResult.put("targetLang", normalizedTarget);
        translatedResult.put("fromCache", false);
        return translatedResult;
    }

    private String normalizeSourceLang(String sourceLang, String text) {
        if (StringUtils.hasText(sourceLang)) {
            return normalizeLangCode(sourceLang);
        }
        return normalizeLangCode(detectLanguage(text));
    }

    private String normalizeTargetLang(String targetLang) {
        if (!StringUtils.hasText(targetLang)) {
            return "zh";
        }
        return normalizeLangCode(targetLang);
    }

    private String normalizeLangCode(String langCode) {
        String code = defaultString(langCode).toLowerCase().replace('_', '-');
        if (!StringUtils.hasText(code)) {
            return "auto";
        }
        if ("zh-cn".equals(code) || "zh-hans".equals(code)) {
            return "zh";
        }
        if ("zh-tw".equals(code) || "zh-hant".equals(code)) {
            return "zh-TW";
        }
        if (code.contains("-")) {
            return code.substring(0, code.indexOf('-'));
        }
        return code;
    }

    private String buildPostCacheKey(Long postId, String targetLang) {
        return "discover:translate:post:" + postId + ":" + normalizeTargetLang(targetLang);
    }

    private String buildCommentCacheKey(Long commentId, String targetLang) {
        return "discover:translate:comment:" + commentId + ":" + normalizeTargetLang(targetLang);
    }

    private void clearPostTranslateCache(Long postId) {
        Set<String> keys = redisTemplate.keys("discover:translate:post:" + postId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void clearCommentTranslateCache(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return;
        }
        Set<String> keys = new HashSet<String>();
        for (Long commentId : commentIds) {
            Set<String> matchedKeys = redisTemplate.keys("discover:translate:comment:" + commentId + ":*");
            if (matchedKeys != null && !matchedKeys.isEmpty()) {
                keys.addAll(matchedKeys);
            }
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String detectLanguage(String text) {
        if (!StringUtils.hasText(text)) {
            return "auto";
        }
        return detectLanguageByChars(text);
    }

    private String detectLanguageByChars(String text) {
        int zhCount = 0;
        int jaCount = 0;
        int koCount = 0;
        int enCount = 0;
        int total = 0;

        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c) || Character.isDigit(c)) {
                continue;
            }
            total++;
            if (c >= 0x4E00 && c <= 0x9FFF) {
                zhCount++;
            } else if (c >= 0x3040 && c <= 0x309F) {
                jaCount++;
            } else if (c >= 0x30A0 && c <= 0x30FF) {
                jaCount++;
            } else if (c >= 0xAC00 && c <= 0xD7AF) {
                koCount++;
            } else if (c >= 0x1100 && c <= 0x11FF) {
                koCount++;
            } else if (c >= 0x3130 && c <= 0x318F) {
                koCount++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                enCount++;
            }
        }

        if (total == 0) {
            return "auto";
        }

        if (jaCount > 0 && jaCount >= zhCount * 0.1) {
            return "ja";
        }
        if (koCount > 0) {
            return "ko";
        }
        if (zhCount > 0 && zhCount > enCount) {
            return "zh";
        }
        if (enCount > 0 && enCount > zhCount) {
            return "en";
        }
        if (zhCount > 0) {
            return "zh";
        }
        if (enCount > 0) {
            return "en";
        }
        return "auto";
    }
}
