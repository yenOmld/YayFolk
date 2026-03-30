package com.yayfolk.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.entity.DiscoverPost;
import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.IntangibleCulturalHeritage;
import com.yayfolk.backend.entity.MerchantApplication;
import com.yayfolk.backend.entity.MerchantProfile;
import com.yayfolk.backend.entity.OfficialContent;
import com.yayfolk.backend.entity.PostReport;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.entity.UserUnbanApplication;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.DiscoverPostRepository;
import com.yayfolk.backend.repository.IntangibleCulturalHeritageRepository;
import com.yayfolk.backend.repository.MerchantApplicationRepository;
import com.yayfolk.backend.repository.MerchantProfileRepository;
import com.yayfolk.backend.repository.OfficialContentRepository;
import com.yayfolk.backend.repository.PostReportRepository;
import com.yayfolk.backend.repository.UserRepository;
import com.yayfolk.backend.repository.UserUnbanApplicationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class AdminService {
    private static final String AUDIT_STATUS_PASSED = "passed";
    private static final String AUDIT_STATUS_REJECTED = "rejected";
    private static final String REPORT_STATUS_PENDING = "pending";
    private static final String REPORT_STATUS_PROCESSED = "processed";
    private static final String HOMEPAGE_ACTIVITY_CATEGORY = "homepage_activity_selection";
    private static final String HOMEPAGE_HERITAGE_CATEGORY = "homepage_heritage_selection";
    private static final String HOMEPAGE_WORK_CATEGORY = "homepage_work_selection";
    private static final int HOMEPAGE_ACTIVITY_LIMIT = 3;
    private static final int HOMEPAGE_HERITAGE_LIMIT = 6;
    private static final int HOMEPAGE_WORK_LIMIT = 6;
    private static final List<String> APPROVED_POST_AUDIT_STATUSES = Arrays.asList(AUDIT_STATUS_PASSED, "approved");

    @Value("${FRONTEND_BASE_URL:http://localhost:5173}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantApplicationRepository applicationRepository;
    private final ActivityRepository activityRepository;
    private final DiscoverPostRepository postRepository;
    private final OfficialContentRepository officialContentRepository;
    private final PostReportRepository postReportRepository;
    private final UserUnbanApplicationRepository userUnbanApplicationRepository;
    private final IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        MerchantProfileRepository merchantProfileRepository,
                        MerchantApplicationRepository applicationRepository,
                        ActivityRepository activityRepository,
                        DiscoverPostRepository postRepository,
                        OfficialContentRepository officialContentRepository,
                        PostReportRepository postReportRepository,
                        UserUnbanApplicationRepository userUnbanApplicationRepository,
                        IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository,
                        EmailService emailService,
                        ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.applicationRepository = applicationRepository;
        this.activityRepository = activityRepository;
        this.postRepository = postRepository;
        this.officialContentRepository = officialContentRepository;
        this.postReportRepository = postReportRepository;
        this.userUnbanApplicationRepository = userUnbanApplicationRepository;
        this.intangibleCulturalHeritageRepository = intangibleCulturalHeritageRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    private User requireAdmin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"admin".equals(user.getRole())) {
            throw new RuntimeException("只有管理员可以访问此资源");
        }
        return user;
    }

    private User requireSuperAdmin(String username) {
        User user = requireAdmin(username);
        if (!isSuperAdmin(user)) {
            throw new RuntimeException("只有超级管理员可以管理管理员账户");
        }
        return user;
    }

    private User requireAdminAccount(Long adminId) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        if (!"admin".equals(user.getRole())) {
            throw new RuntimeException("目标用户不是管理员");
        }
        return user;
    }

    private boolean isSuperAdmin(User user) {
        return user != null && Integer.valueOf(1).equals(user.getIsSuperAdmin());
    }

    private String getRequiredString(Map<String, Object> data, String key, String message) {
        Object value = data.get(key);
        if (value == null) {
            throw new RuntimeException(message);
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            throw new RuntimeException(message);
        }
        return text;
    }

    private String getOptionalString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private void ensureUniqueAdminEmail(String email, Long ignoreUserId) {
        if (email == null || email.isEmpty()) {
            return;
        }
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (ignoreUserId == null || !existing.getId().equals(ignoreUserId)) {
                throw new RuntimeException("Email already exists");
            }
        });
    }

    // Merchant applications
    public List<Map<String, Object>> getMerchantApplications(String adminUsername, String status) {
        requireAdmin(adminUsername);

        List<MerchantApplication> apps;
        if (status != null && !status.isEmpty()) {
            apps = applicationRepository.findByApplicationStatusOrderByCreateTimeDesc(status);
        } else {
            apps = applicationRepository.findAllByOrderByCreateTimeDesc();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (MerchantApplication app : apps) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", app.getId());
            m.put("userId", app.getUserId());
            m.put("realName", app.getRealName());
            m.put("phone", app.getPhone());
            m.put("heritageType", app.getHeritageType());
            m.put("shopName", app.getShopName());
            m.put("shopAddress", app.getShopAddress());
            m.put("heritageDescription", app.getHeritageDescription());
            m.put("applicationStatus", app.getApplicationStatus());
            m.put("auditRemark", app.getAuditRemark());
            m.put("createTime", app.getCreateTime());
            m.put("merchantProfileId", app.getMerchantProfileId());
            userRepository.findById(app.getUserId()).ifPresent(u -> {
                m.put("username", u.getUsername());
                m.put("nickname", u.getNickname());
                m.put("avatar", u.getAvatar());
            });
            result.add(m);
        }
        return result;
    }

    public Map<String, Object> auditMerchantApplication(String adminUsername, Long appId,
                                                         boolean approve, String remark) {
        User admin = requireAdmin(adminUsername);
        MerchantApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Merchant application does not exist"));

        app.setApplicationStatus(approve ? "approved" : "rejected");
        String trimmedRemark = remark == null ? null : remark.trim();
        if (!approve && (trimmedRemark == null || trimmedRemark.isEmpty())) {
            throw new RuntimeException("请提供拒绝原因");
        }
        app.setAuditRemark(approve ? null : trimmedRemark);
        app.setAuditAdminId(admin.getId());
        app.setAuditTime(new Date());
        applicationRepository.save(app);

        User applicant = userRepository.findById(app.getUserId())
                .orElseThrow(() -> new RuntimeException("申请人不存在"));
        if (approve) {
            applicant.setRole("merchant");
            applicant.setShopStatus("approved");
            applicant.setIsMerchant(1);
            if (app.getShopName() != null) {
                applicant.setShopName(app.getShopName());
            }
        } else {
            applicant.setShopStatus("rejected");
        }
        userRepository.save(applicant);

        MerchantProfile profile = merchantProfileRepository.findByUserId(app.getUserId())
                .orElseGet(() -> {
                    MerchantProfile newProfile = new MerchantProfile();
                    newProfile.setUserId(app.getUserId());
                    return newProfile;
                });

        if (approve) {
            profile.setBusinessStatus("approved");
            profile.setShopName(app.getShopName() != null ? app.getShopName() : applicant.getShopName());
            profile.setContactName(app.getRealName());
            profile.setContactPhone(app.getPhone());
            profile.setHeritageType(app.getHeritageType());
            profile.setHeritageDescription(app.getHeritageDescription());
            profile.setAddress(app.getShopAddress());
            profile.setProvince(app.getProvince());
            profile.setCity(app.getCity());
            profile.setShopIntro(app.getIntro());
            profile.setLatestApplicationId(app.getId());
        } else {
            profile.setBusinessStatus("rejected");
        }
        merchantProfileRepository.save(profile);

        app.setMerchantProfileId(profile.getId());
        applicationRepository.save(app);

        Map<String, Object> result = new HashMap<>();
        result.put("id", app.getId());
        result.put("applicationStatus", app.getApplicationStatus());
        result.put("merchantProfileId", profile.getId());
        result.put("businessStatus", profile.getBusinessStatus());
        return result;
    }

    // Activity moderation
    public List<Map<String, Object>> getActivities(String adminUsername, String auditStatus) {
        requireAdmin(adminUsername);

        List<Activity> activities;
        if (auditStatus != null && !auditStatus.isEmpty()) {
            activities = activityRepository.findByAuditStatusOrderByCreateTimeDesc(auditStatus);
        } else {
            activities = activityRepository.findAll();
            activities.sort((a, b) -> {
                Date at = a.getCreateTime();
                Date bt = b.getCreateTime();
                if (at == null && bt == null) return 0;
                if (at == null) return 1;
                if (bt == null) return -1;
                return bt.compareTo(at);
            });
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Activity activity : activities) {
            result.add(activityToMap(activity));
        }
        return result;
    }

    public Map<String, Object> auditActivity(String adminUsername, Long activityId, boolean approve, String remark) {
        User admin = requireAdmin(adminUsername);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));

        activity.setAuditStatus(approve ? "approved" : "rejected");
        String trimmedRemark = remark == null ? null : remark.trim();
        if (!approve && (trimmedRemark == null || trimmedRemark.isEmpty())) {
            throw new RuntimeException("请提供拒绝原因");
        }
        activity.setAuditRemark(approve ? null : trimmedRemark);
        activityRepository.save(activity);

        Map<String, Object> result = new HashMap<>();
        result.put("id", activity.getId());
        result.put("auditStatus", activity.getAuditStatus());
        result.put("auditRemark", activity.getAuditRemark());
        result.put("auditAdminId", admin.getId());
        return result;
    }

    // Post moderation
    public List<Map<String, Object>> getPosts(String adminUsername, String auditStatus, int page, int size) {
        requireAdmin(adminUsername);

        List<DiscoverPost> posts;
        if (auditStatus != null && !auditStatus.isEmpty()) {
            posts = postRepository.findByStatusAndAuditStatusOrderByCreateTimeDesc(1, auditStatus);
        } else {
            posts = postRepository.findByStatusOrderByCreateTimeDesc(1);
        }
        Map<Long, Integer> pendingReportCountMap = buildPendingReportCountMap(posts);
        posts.sort((left, right) -> {
            int leftCount = pendingReportCountMap.getOrDefault(left.getId(), 0);
            int rightCount = pendingReportCountMap.getOrDefault(right.getId(), 0);
            if (leftCount != rightCount) {
                return Integer.compare(rightCount, leftCount);
            }
            Date leftTime = left.getCreateTime();
            Date rightTime = right.getCreateTime();
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        });

        List<Map<String, Object>> result = new ArrayList<>();
        int start = Math.max(page, 0) * size;
        int end = Math.min(start + size, posts.size());
        if (start >= posts.size()) {
            return result;
        }

        for (int i = start; i < end; i++) {
            DiscoverPost p = posts.get(i);
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("title", p.getTitle());
            m.put("content", p.getContent());
            m.put("images", p.getImages());
            m.put("category", p.getCategory());
            m.put("auditStatus", p.getAuditStatus());
            m.put("auditRemark", p.getAuditRemark());
            m.put("pendingReportCount", pendingReportCountMap.getOrDefault(p.getId(), 0));
            m.put("createTime", p.getCreateTime());
            userRepository.findById(p.getUserId()).ifPresent(u -> {
                m.put("username", u.getUsername());
                m.put("nickname", u.getNickname());
            });
            result.add(m);
        }
        return result;
    }

    public Map<String, Object> auditPost(String adminUsername, Long postId, boolean pass, String remark) {
        User admin = requireAdmin(adminUsername);
        DiscoverPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        String trimmedRemark = remark == null ? null : remark.trim();
        if (!pass && (trimmedRemark == null || trimmedRemark.isEmpty())) {
            throw new RuntimeException("请提供拒绝原因");
        }
        post.setAuditStatus(pass ? AUDIT_STATUS_PASSED : AUDIT_STATUS_REJECTED);
        post.setAuditRemark(pass ? null : trimmedRemark);
        postRepository.save(post);
        resolvePendingReports(postId, admin.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", post.getId());
        result.put("auditStatus", post.getAuditStatus());
        result.put("auditRemark", post.getAuditRemark());
        return result;
    }

    public Map<String, Object> batchAuditPosts(String adminUsername, List<Long> postIds, boolean pass, String remark) {
        User admin = requireAdmin(adminUsername);
        if (postIds == null || postIds.isEmpty()) {
            throw new RuntimeException("请先选择帖子");
        }
        String trimmedRemark = remark == null ? null : remark.trim();
        if (!pass && (trimmedRemark == null || trimmedRemark.isEmpty())) {
            throw new RuntimeException("请提供拒绝原因");
        }

        List<Long> successIds = new ArrayList<Long>();
        List<Map<String, Object>> failures = new ArrayList<Map<String, Object>>();
        for (Long postId : postIds) {
            if (postId == null) {
                continue;
            }
            try {
                DiscoverPost post = postRepository.findById(postId)
                        .orElseThrow(() -> new RuntimeException("帖子不存在"));
                post.setAuditStatus(pass ? AUDIT_STATUS_PASSED : AUDIT_STATUS_REJECTED);
                post.setAuditRemark(pass ? null : trimmedRemark);
                postRepository.save(post);
                resolvePendingReports(postId, admin.getId());
                successIds.add(postId);
            } catch (Exception e) {
                Map<String, Object> failed = new HashMap<String, Object>();
                failed.put("id", postId);
                failed.put("message", e.getMessage());
                failures.add(failed);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("successIds", successIds);
        result.put("failed", failures);
        result.put("successCount", successIds.size());
        result.put("failedCount", failures.size());
        return result;
    }

    private Map<Long, Integer> buildPendingReportCountMap(List<DiscoverPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<Long> postIds = new ArrayList<Long>();
            for (DiscoverPost post : posts) {
                if (post != null && post.getId() != null) {
                    postIds.add(post.getId());
                }
            }
            if (postIds.isEmpty()) {
                return Collections.emptyMap();
            }
            List<PostReport> reports = postReportRepository.findByPostIdInAndStatus(postIds, REPORT_STATUS_PENDING);
            Map<Long, Integer> countMap = new HashMap<Long, Integer>();
            for (PostReport report : reports) {
                Long postId = report.getPostId();
                if (postId == null) {
                    continue;
                }
                countMap.put(postId, countMap.getOrDefault(postId, 0) + 1);
            }
            return countMap;
        } catch (DataAccessException e) {
            log.warn("查询待处理举报数量失败;回退到零计数", e);
            return Collections.emptyMap();
        }
    }

    private void resolvePendingReports(Long postId, Long handlerId) {
        try {
            List<PostReport> pendingReports = postReportRepository.findByPostIdAndStatus(postId, REPORT_STATUS_PENDING);
            if (pendingReports.isEmpty()) {
                return;
            }
            Date now = new Date();
            for (PostReport report : pendingReports) {
                report.setStatus(REPORT_STATUS_PROCESSED);
                report.setHandlerId(handlerId);
                report.setHandleTime(now);
            }
            postReportRepository.saveAll(pendingReports);
        } catch (DataAccessException e) {
            log.warn("解析帖子{}的待处理举报失败", postId, e);
        }
    }

    // User management
    public Map<String, Object> getUsers(String adminUsername, int page, int size, String keyword) {
        requireAdmin(adminUsername);

        List<User> all = userRepository.findAll();
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (User u : all) {
            if ("admin".equalsIgnoreCase(u.getRole()) || isSuperAdmin(u)) {
                continue;
            }
            if (keyword != null && !keyword.isEmpty()) {
                boolean match = (u.getUsername() != null && u.getUsername().contains(keyword))
                        || (u.getNickname() != null && u.getNickname().contains(keyword))
                        || (u.getEmail() != null && u.getEmail().contains(keyword));
                if (!match) {
                    continue;
                }
            }
            filtered.add(userToMap(u));
        }

        int total = filtered.size();
        int start = Math.max(page, 0) * size;
        int end = Math.min(start + size, total);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", start >= total ? new ArrayList<>() : filtered.subList(start, end));
        return result;
    }

    public void banUser(String adminUsername, Long userId, String reason) {
        User admin = requireAdmin(adminUsername);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if ("admin".equalsIgnoreCase(user.getRole()) || isSuperAdmin(user)) {
            throw new RuntimeException("管理员账户不能被禁用");
        }

        String trimmedReason = reason == null ? null : reason.trim();
        if (trimmedReason == null || trimmedReason.isEmpty()) {
            throw new RuntimeException("请提供封禁原因");
        }

        user.setStatus(0);
        user.setBanReason(trimmedReason);
        user.setBanAdminId(admin.getId());
        user.setBanTime(new Date());
        userRepository.save(user);

        sendBanEmail(user, trimmedReason);
    }

    public void unbanUser(String adminUsername, Long userId) {
        requireAdmin(adminUsername);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if ("admin".equalsIgnoreCase(user.getRole()) || isSuperAdmin(user)) {
            throw new RuntimeException("管理员账户不能通过此操作恢复");
        }
        user.setStatus(1);
        user.setBanReason(null);
        user.setBanAdminId(null);
        user.setBanTime(null);
        userRepository.save(user);

        sendUnbanEmail(user, null);
    }

    public List<Map<String, Object>> getUnbanApplications(String adminUsername, String status) {
        requireAdmin(adminUsername);

        List<UserUnbanApplication> applications;
        if (status != null && !status.trim().isEmpty()) {
            applications = userUnbanApplicationRepository.findByStatusOrderByCreateTimeDesc(status.trim());
        } else {
            applications = userUnbanApplicationRepository.findAllByOrderByCreateTimeDesc();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserUnbanApplication application : applications) {
            result.add(unbanApplicationToMap(application));
        }
        return result;
    }

    public Map<String, Object> auditUnbanApplication(String adminUsername,
                                                     Long applicationId,
                                                     boolean approve,
                                                     String remark) {
        User admin = requireAdmin(adminUsername);
        UserUnbanApplication application = userUnbanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("解封申请不存在"));

        if (!"pending".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("此解封申请已被处理");
        }

        User targetUser = userRepository.findById(application.getUserId())
                .orElseThrow(() -> new RuntimeException("目标用户不存在"));

        String trimmedRemark = remark == null ? null : remark.trim();
        if (!approve && (trimmedRemark == null || trimmedRemark.isEmpty())) {
            throw new RuntimeException("请提供拒绝原因");
        }

        application.setAdminId(admin.getId());
        application.setHandleTime(new Date());
        application.setAdminRemark(trimmedRemark);

        if (approve) {
            application.setStatus("approved");
            targetUser.setStatus(1);
            targetUser.setBanReason(null);
            targetUser.setBanAdminId(null);
            targetUser.setBanTime(null);
            userRepository.save(targetUser);
            sendUnbanEmail(targetUser, trimmedRemark);
        } else {
            application.setStatus("rejected");
        }

        UserUnbanApplication saved = userUnbanApplicationRepository.save(application);
        return unbanApplicationToMap(saved);
    }

    private void sendBanEmail(User user, String reason) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return;
        }

        String subject = "YayFolk账户通知";
        StringBuilder content = new StringBuilder();
        content.append("您的YayFolk账户已被禁用。\n");
        content.append("原因: ").append(reason);
        content.append("\n\n如果您认为这是误操作，可以在此提交解封申请:\n");
        content.append(buildUnbanApplicationUrl(user));
        emailService.sendSystemEmail(user.getEmail(), subject, content.toString());
    }

    private void sendUnbanEmail(User user, String remark) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return;
        }

        String subject = "YayFolk账户已恢复";
        StringBuilder content = new StringBuilder();
        content.append("您的YayFolk账户已恢复。");
        if (remark != null && !remark.trim().isEmpty()) {
            content.append("\n备注: ").append(remark.trim());
        }
        content.append("\n\n您现在可以登录并继续使用YayFolk。");
        emailService.sendSystemEmail(user.getEmail(), subject, content.toString());
    }

    private String buildUnbanApplicationUrl(User user) {
        String baseUrl = frontendBaseUrl == null ? "http://localhost:5173" : frontendBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String account = user.getEmail() != null && !user.getEmail().trim().isEmpty() ? user.getEmail() : user.getUsername();
        try {
            return baseUrl + "/unban-application?account=" + java.net.URLEncoder.encode(account, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return baseUrl + "/unban-application";
        }
    }

    // Official content
    // Official content
    public List<Map<String, Object>> getOfficialContents(String adminUsername) {
        requireAdmin(adminUsername);

        List<OfficialContent> list = officialContentRepository.findAllByOrderByCreateTimeDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (OfficialContent c : list) {
            result.add(contentToMap(c));
        }
        return result;
    }

    public Map<String, Object> createOfficialContent(String adminUsername, Map<String, Object> data) {
        User admin = requireAdmin(adminUsername);
        OfficialContent content = new OfficialContent();
        content.setAdminId(admin.getId());
        content.setTitle((String) data.get("title"));
        content.setContent((String) data.get("content"));
        content.setCategory(data.get("category") != null ? (String) data.get("category") : "introduction");
        content.setCoverImage((String) data.get("coverImage"));
        officialContentRepository.save(content);
        return contentToMap(content);
    }

    public void deleteOfficialContent(String adminUsername, Long id) {
        requireAdmin(adminUsername);
        OfficialContent content = officialContentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("官方内容不存在"));
        officialContentRepository.delete(content);
    }

    // Administrator management
    public List<Map<String, Object>> getAdmins(String adminUsername) {
        requireSuperAdmin(adminUsername);
        List<User> admins = userRepository.findByRoleOrderByCreateTimeDesc("admin");
        List<Map<String, Object>> result = new ArrayList<>();
        for (User admin : admins) {
            result.add(userToMap(admin));
        }
        return result;
    }

    public Map<String, Object> createAdmin(String adminUsername, Map<String, Object> data) {
        requireSuperAdmin(adminUsername);

        String username = getRequiredString(data, "username", "管理员用户名不能为空");
        String password = getRequiredString(data, "password", "管理员密码不能为空");
        String confirmPassword = getRequiredString(data, "confirmPassword", "请确认管理员密码");
        String nickname = getOptionalString(data, "nickname");
        String email = getOptionalString(data, "email");

        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("两次密码不匹配");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        ensureUniqueAdminEmail(email, null);

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole("admin");
        admin.setStatus(1);
        admin.setIsSuperAdmin(0);
        admin.setIsMerchant(0);
        admin.setNickname(nickname != null ? nickname : username);
        admin.setEmail(email);
        admin.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + username);

        return userToMap(userRepository.save(admin));
    }

    public Map<String, Object> updateAdmin(String adminUsername, Long adminId, Map<String, Object> data) {
        requireSuperAdmin(adminUsername);
        User admin = requireAdminAccount(adminId);

        String nickname = getOptionalString(data, "nickname");
        String email = getOptionalString(data, "email");

        ensureUniqueAdminEmail(email, admin.getId());
        admin.setNickname(nickname != null ? nickname : admin.getUsername());
        admin.setEmail(email);

        return userToMap(userRepository.save(admin));
    }

    public Map<String, Object> updateAdminPassword(String adminUsername, Long adminId,
                                                   String newPassword, String confirmPassword) {
        requireSuperAdmin(adminUsername);
        User admin = requireAdminAccount(adminId);

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("新密码不能为空");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("两次密码不匹配");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
        return userToMap(admin);
    }

    public void deleteAdmin(String adminUsername, Long adminId) {
        User operator = requireSuperAdmin(adminUsername);
        User admin = requireAdminAccount(adminId);

        if (operator.getId().equals(admin.getId())) {
            throw new RuntimeException("超级管理员不能删除当前登录的账户");
        }
        if (isSuperAdmin(admin)) {
            throw new RuntimeException("超级管理员账户不能被删除");
        }

        userRepository.delete(admin);
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole());
        m.put("shopStatus", u.getShopStatus());
        m.put("shopName", u.getShopName());
        m.put("isMerchant", u.getIsMerchant());
        m.put("status", u.getStatus());
        m.put("banReason", u.getBanReason());
        m.put("banAdminId", u.getBanAdminId());
        m.put("banTime", u.getBanTime());
        m.put("isSuperAdmin", u.getIsSuperAdmin());
        m.put("createTime", u.getCreateTime());
        return m;
    }

    private Map<String, Object> unbanApplicationToMap(UserUnbanApplication application) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", application.getId());
        m.put("userId", application.getUserId());
        m.put("applyReason", application.getApplyReason());
        m.put("status", application.getStatus());
        m.put("adminId", application.getAdminId());
        m.put("adminRemark", application.getAdminRemark());
        m.put("createTime", application.getCreateTime());
        m.put("handleTime", application.getHandleTime());

        userRepository.findById(application.getUserId()).ifPresent(user -> {
            m.put("username", user.getUsername());
            m.put("nickname", user.getNickname());
            m.put("email", user.getEmail());
            m.put("banReason", user.getBanReason());
            m.put("userStatus", user.getStatus());
        });

        if (application.getAdminId() != null) {
            userRepository.findById(application.getAdminId())
                    .ifPresent(admin -> m.put("adminUsername", admin.getUsername()));
        }

        return m;
    }

    private Map<String, Object> contentToMap(OfficialContent c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("content", c.getContent());
        m.put("category", c.getCategory());
        m.put("coverImage", c.getCoverImage());
        m.put("isPublic", c.getIsPublic());
        m.put("viewCount", c.getViewCount());
        m.put("createTime", c.getCreateTime());
        return m;
    }

    private Map<String, Object> activityToMap(Activity activity) {
        Map<String, Object> m = new HashMap<>();
        List<String> imageList = parseStringList(activity.getImages());
        m.put("id", activity.getId());
        m.put("merchantId", activity.getMerchantId());
        m.put("merchantProfileId", activity.getMerchantProfileId());
        m.put("title", activity.getTitle());
        m.put("subtitle", activity.getSubtitle());
        m.put("coverImage", activity.getCoverImage());
        m.put("images", imageList);
        m.put("imageList", imageList);
        m.put("heritageType", activity.getHeritageType());
        m.put("activityType", activity.getActivityType());
        m.put("startTime", activity.getStartTime());
        m.put("endTime", activity.getEndTime());
        m.put("price", activity.getPrice());
        m.put("maxParticipants", activity.getMaxParticipants());
        m.put("currentParticipants", activity.getCurrentParticipants());
        m.put("locationProvince", activity.getLocationProvince());
        m.put("locationCity", activity.getLocationCity());
        m.put("locationDistrict", activity.getLocationDistrict());
        m.put("locationDetail", activity.getLocationDetail());
        m.put("status", activity.getStatus());
        m.put("auditStatus", activity.getAuditStatus());
        m.put("auditRemark", activity.getAuditRemark());
        m.put("content", activity.getContent());
        m.put("viewCount", safeInt(activity.getViewCount()));
        m.put("createTime", activity.getCreateTime());
        userRepository.findById(activity.getMerchantId()).ifPresent(user -> {
            m.put("merchantName", user.getShopName() != null ? user.getShopName() : user.getNickname());
            m.put("merchantAvatar", user.getAvatar());
            m.put("merchantUsername", user.getUsername());
        });
        return m;
    }

    public List<Map<String, Object>> getOfficialActivities(String adminUsername) {
        requireAdmin(adminUsername);
        List<Long> publishedIds = readPublishedIds(HOMEPAGE_ACTIVITY_CATEGORY);
        List<Activity> activities = activityRepository.findByAuditStatusAndStatusNotOrderByStartTimeAsc("approved", "ended");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Activity activity : activities) {
            Map<String, Object> item = activityToMap(activity);
            item.put("description", activity.getContent());
            item.put("published", publishedIds.contains(activity.getId()));
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> createOfficialActivity(String adminUsername, Map<String, Object> data) {
        User admin = requireAdmin(adminUsername);
        Activity activity = new Activity();
        activity.setMerchantId(admin.getId());
        applyActivityPayload(activity, data, false);
        if (normalizeText(activity.getTitle()) == null) throw new RuntimeException("活动标题不能为空");
        if (activity.getStartTime() == null || activity.getEndTime() == null) throw new RuntimeException("开始时间和结束时间不能为空");
        activity.setAuditStatus("approved");
        activity.setAuditRemark(null);
        activityRepository.save(activity);
        return activityToMap(activity);
    }

    public Map<String, Object> updateOfficialActivity(String adminUsername, Long id, Map<String, Object> data) {
        requireAdmin(adminUsername);
        Activity activity = activityRepository.findById(id).orElseThrow(() -> new RuntimeException("活动不存在"));
        applyActivityPayload(activity, data, true);
        if (normalizeText(activity.getTitle()) == null) throw new RuntimeException("活动标题不能为空");
        if (activity.getStartTime() == null || activity.getEndTime() == null) throw new RuntimeException("开始时间和结束时间不能为空");
        activity.setAuditStatus("approved");
        activity.setAuditRemark(null);
        activityRepository.save(activity);
        return activityToMap(activity);
    }

    public void deleteOfficialActivity(String adminUsername, Long id) {
        requireAdmin(adminUsername);
        Activity activity = activityRepository.findById(id).orElseThrow(() -> new RuntimeException("活动不存在"));
        activityRepository.delete(activity);
    }

    public List<Map<String, Object>> getOfficialHeritages(String adminUsername) {
        requireAdmin(adminUsername);
        List<Long> publishedIds = readPublishedIds(HOMEPAGE_HERITAGE_CATEGORY);
        List<IntangibleCulturalHeritage> heritages = intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (IntangibleCulturalHeritage heritage : heritages) {
            result.add(heritageToAdminMap(heritage, publishedIds));
        }
        return result;
    }

    public Map<String, Object> createOfficialHeritage(String adminUsername, Map<String, Object> data) {
        requireAdmin(adminUsername);
        IntangibleCulturalHeritage heritage = new IntangibleCulturalHeritage();
        applyHeritagePayload(heritage, data, false);
        if (normalizeText(heritage.getName()) == null) throw new RuntimeException("非遗名称不能为空");
        intangibleCulturalHeritageRepository.save(heritage);
        return heritageToAdminMap(heritage, Collections.<Long>emptyList());
    }

    public Map<String, Object> updateOfficialHeritage(String adminUsername, Long id, Map<String, Object> data) {
        requireAdmin(adminUsername);
        IntangibleCulturalHeritage heritage = intangibleCulturalHeritageRepository.findById(id).orElseThrow(() -> new RuntimeException("非遗不存在"));
        applyHeritagePayload(heritage, data, true);
        if (normalizeText(heritage.getName()) == null) throw new RuntimeException("非遗名称不能为空");
        intangibleCulturalHeritageRepository.save(heritage);
        return heritageToAdminMap(heritage, readPublishedIds(HOMEPAGE_HERITAGE_CATEGORY));
    }

    public void deleteOfficialHeritage(String adminUsername, Long id) {
        requireAdmin(adminUsername);
        IntangibleCulturalHeritage heritage = intangibleCulturalHeritageRepository.findById(id).orElseThrow(() -> new RuntimeException("非遗不存在"));
        intangibleCulturalHeritageRepository.delete(heritage);
    }

    public List<Map<String, Object>> getOfficialWorks(String adminUsername) {
        requireAdmin(adminUsername);
        List<Long> publishedIds = readPublishedIds(HOMEPAGE_WORK_CATEGORY);
        List<DiscoverPost> posts = postRepository.findByStatusAndAuditStatusInOrderByCreateTimeDesc(1, APPROVED_POST_AUDIT_STATUSES);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DiscoverPost post : posts) {
            result.add(workToAdminMap(post, publishedIds));
        }
        result.sort((left, right) -> Integer.valueOf(safeInt(right.get("heat"))).compareTo(safeInt(left.get("heat"))));
        if (result.size() > 20) return new ArrayList<>(result.subList(0, 20));
        return result;
    }

    public Map<String, Object> createOfficialWork(String adminUsername, Map<String, Object> data) {
        User admin = requireAdmin(adminUsername);
        DiscoverPost post = new DiscoverPost();
        post.setUserId(admin.getId());
        applyWorkPayload(post, data, false);
        if (normalizeText(post.getTitle()) == null) throw new RuntimeException("作品标题不能为空");
        if (normalizeText(post.getContent()) == null) throw new RuntimeException("作品内容不能为空");
        post.setStatus(1);
        post.setAuditStatus(AUDIT_STATUS_PASSED);
        post.setAuditRemark(null);
        postRepository.save(post);
        return workToAdminMap(post, Collections.<Long>emptyList());
    }

    public Map<String, Object> updateOfficialWork(String adminUsername, Long id, Map<String, Object> data) {
        requireAdmin(adminUsername);
        DiscoverPost post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("作品不存在"));
        applyWorkPayload(post, data, true);
        if (normalizeText(post.getTitle()) == null) throw new RuntimeException("作品标题不能为空");
        if (normalizeText(post.getContent()) == null) throw new RuntimeException("作品内容不能为空");
        post.setStatus(1);
        post.setAuditStatus(AUDIT_STATUS_PASSED);
        post.setAuditRemark(null);
        postRepository.save(post);
        return workToAdminMap(post, readPublishedIds(HOMEPAGE_WORK_CATEGORY));
    }

    public void deleteOfficialWork(String adminUsername, Long id) {
        requireAdmin(adminUsername);
        DiscoverPost post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("作品不存在"));
        post.setStatus(0);
        postRepository.save(post);
    }

    public Map<String, Object> getHomepagePublishedState(String adminUsername) {
        requireAdmin(adminUsername);
        Map<String, Object> result = new HashMap<>();
        result.put("activities", readPublishedIds(HOMEPAGE_ACTIVITY_CATEGORY));
        result.put("heritages", readPublishedIds(HOMEPAGE_HERITAGE_CATEGORY));
        result.put("works", readPublishedIds(HOMEPAGE_WORK_CATEGORY));
        return result;
    }

    public Map<String, Object> publishToHomepage(String adminUsername, String type, List<Long> ids) {
        User admin = requireAdmin(adminUsername);
        String normalizedType = type == null ? "" : type.trim().toLowerCase();
        List<Long> normalizedIds = normalizeIdList(ids);
        validateHomepagePublish(normalizedType, normalizedIds);
        OfficialContent config = new OfficialContent();
        config.setAdminId(admin.getId());
        config.setTitle("homepage_publish_" + normalizedType);
        config.setCategory(resolveHomepageCategory(normalizedType));
        config.setContent(writeJson(normalizedIds));
        config.setIsPublic(1);
        config.setViewCount(0);
        officialContentRepository.save(config);
        Map<String, Object> result = new HashMap<>();
        result.put("type", normalizedType);
        result.put("ids", normalizedIds);
        return result;
    }

    private void validateHomepagePublish(String type, List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new RuntimeException("请至少选择一项");
        if ("activity".equals(type)) {
            if (ids.size() > HOMEPAGE_ACTIVITY_LIMIT) throw new RuntimeException("最多只能发布3个活动");
            ensureIdsExist(ids, extractActivityIds(activityRepository.findByAuditStatusAndStatusNotOrderByStartTimeAsc("approved", "ended")), "某些活动不可用");
            return;
        }
        if ("heritage".equals(type)) {
            if (ids.size() > HOMEPAGE_HERITAGE_LIMIT) throw new RuntimeException("最多只能发布6个非遗项目");
            ensureIdsExist(ids, extractHeritageIds(intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc()), "某些非遗项目不可用");
            return;
        }
        if ("work".equals(type)) {
            if (ids.size() > HOMEPAGE_WORK_LIMIT) throw new RuntimeException("最多只能发布6个作品");
            List<DiscoverPost> posts = postRepository.findByStatusAndAuditStatusInOrderByCreateTimeDesc(1, APPROVED_POST_AUDIT_STATUSES);
            List<Map<String, Object>> works = new ArrayList<>();
            for (DiscoverPost post : posts) works.add(workToAdminMap(post, Collections.<Long>emptyList()));
            works.sort((left, right) -> Integer.valueOf(safeInt(right.get("heat"))).compareTo(safeInt(left.get("heat"))));
            if (works.size() > 20) works = new ArrayList<>(works.subList(0, 20));
            List<Long> validIds = new ArrayList<>();
            for (Map<String, Object> item : works) {
                Long workId = toLong(item.get("id"));
                if (workId != null) validIds.add(workId);
            }
            ensureIdsExist(ids, validIds, "作品必须来自前20个已批准的热门帖子");
            return;
        }
        throw new RuntimeException("不支持的发布类型");
    }

    private void ensureIdsExist(List<Long> selectedIds, List<Long> validIds, String message) {
        for (Long id : selectedIds) {
            if (!validIds.contains(id)) throw new RuntimeException(message);
        }
    }

    private String resolveHomepageCategory(String type) {
        if ("activity".equals(type)) return HOMEPAGE_ACTIVITY_CATEGORY;
        if ("heritage".equals(type)) return HOMEPAGE_HERITAGE_CATEGORY;
        if ("work".equals(type)) return HOMEPAGE_WORK_CATEGORY;
        throw new RuntimeException("不支持的发布类型");
    }

    private List<Long> readPublishedIds(String category) {
        OfficialContent content = officialContentRepository.findTopByCategoryAndIsPublicOrderByCreateTimeDesc(category, 1);
        if (content == null || content.getContent() == null || content.getContent().trim().isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(content.getContent(), new TypeReference<List<Long>>() { });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void applyActivityPayload(Activity activity, Map<String, Object> data, boolean partial) {
        if (!partial || data.containsKey("title")) activity.setTitle(normalizeText(data.get("title")));
        if (!partial || data.containsKey("subtitle")) activity.setSubtitle(normalizeText(data.get("subtitle")));
        if (!partial || data.containsKey("content")) activity.setContent(normalizeText(data.get("content")));
        if (!partial || data.containsKey("heritageType")) activity.setHeritageType(normalizeText(data.get("heritageType")));
        if (!partial || data.containsKey("activityType")) activity.setActivityType(normalizeText(data.get("activityType")));
        if (!partial || data.containsKey("locationProvince")) activity.setLocationProvince(normalizeText(data.get("locationProvince")));
        if (!partial || data.containsKey("locationCity")) activity.setLocationCity(normalizeText(data.get("locationCity")));
        if (!partial || data.containsKey("locationDistrict")) activity.setLocationDistrict(normalizeText(data.get("locationDistrict")));
        if (!partial || data.containsKey("locationDetail")) activity.setLocationDetail(normalizeText(data.get("locationDetail")));
        if (!partial || data.containsKey("price")) activity.setPrice(defaultZero(toNullableInteger(data.get("price"))));
        if (!partial || data.containsKey("maxParticipants")) activity.setMaxParticipants(toNullableInteger(data.get("maxParticipants")));
        if (!partial || data.containsKey("startTime")) activity.setStartTime(parseDateValue(data.get("startTime")));
        if (!partial || data.containsKey("endTime")) activity.setEndTime(parseDateValue(data.get("endTime")));
        List<String> imageList = null;
        if (!partial || data.containsKey("images")) {
            imageList = normalizeStringList(data.get("images"));
            activity.setImages(imageList.isEmpty() ? null : writeJson(imageList));
        }
        if (!partial || data.containsKey("coverImage")) activity.setCoverImage(normalizeText(data.get("coverImage")));
        if ((activity.getCoverImage() == null || activity.getCoverImage().trim().isEmpty()) && imageList != null && !imageList.isEmpty()) activity.setCoverImage(imageList.get(0));
    }

    private void applyHeritagePayload(IntangibleCulturalHeritage heritage, Map<String, Object> data, boolean partial) {
        if (!partial || data.containsKey("name")) heritage.setName(normalizeText(data.get("name")));
        if (!partial || data.containsKey("category")) heritage.setCategory(normalizeText(data.get("category")));
        if (!partial || data.containsKey("subcategory")) heritage.setSubcategory(normalizeText(data.get("subcategory")));
        if (!partial || data.containsKey("dynasty")) heritage.setDynasty(normalizeText(data.get("dynasty")));
        if (!partial || data.containsKey("region")) heritage.setRegion(normalizeText(data.get("region")));
        if (!partial || data.containsKey("level")) heritage.setLevel(normalizeText(data.get("level")));
        if (!partial || data.containsKey("introduction")) heritage.setIntroduction(normalizeText(data.get("introduction")));
        if (!partial || data.containsKey("history")) heritage.setHistory(normalizeText(data.get("history")));
        if (!partial || data.containsKey("inheritanceValue")) heritage.setInheritanceValue(normalizeText(data.get("inheritanceValue")));
        if (!partial || data.containsKey("representativeInheritor")) heritage.setRepresentativeInheritor(normalizeText(data.get("representativeInheritor")));
        if (!partial || data.containsKey("images")) heritage.setImages(writeStringList(data.get("images")));
        if (!partial || data.containsKey("videoUrl")) heritage.setVideoUrl(normalizeText(data.get("videoUrl")));
        if (!partial || data.containsKey("isFeatured")) heritage.setIsFeatured(toFlag(data.get("isFeatured")));
    }

    private void applyWorkPayload(DiscoverPost post, Map<String, Object> data, boolean partial) {
        if (!partial || data.containsKey("title")) post.setTitle(normalizeText(data.get("title")));
        if (!partial || data.containsKey("content")) post.setContent(normalizeText(data.get("content")));
        if (!partial || data.containsKey("category")) post.setCategory(normalizeText(data.get("category")));
        if (!partial || data.containsKey("sourceLang")) post.setSourceLang(normalizeText(data.get("sourceLang")));
        if (!partial || data.containsKey("images")) post.setImages(writeStringList(data.get("images")));
        if (!partial || data.containsKey("tags")) post.setTags(writeStringList(data.get("tags")));
        if (!partial || data.containsKey("viewCount")) { Integer value = toNullableInteger(data.get("viewCount")); if (value != null) post.setViewCount(value); }
        if (!partial || data.containsKey("collectCount")) { Integer value = toNullableInteger(data.get("collectCount")); if (value != null) { post.setCollectCount(value); post.setCollectionCount(value); } }
        if (!partial || data.containsKey("commentCount")) { Integer value = toNullableInteger(data.get("commentCount")); if (value != null) post.setCommentCount(value); }
    }

    private Map<String, Object> heritageToAdminMap(IntangibleCulturalHeritage heritage, List<Long> publishedIds) {
        Map<String, Object> item = new HashMap<>();
        List<String> imageList = parseStringList(heritage.getImages());
        item.put("id", heritage.getId());
        item.put("name", heritage.getName());
        item.put("title", heritage.getName());
        item.put("category", heritage.getCategory());
        item.put("subcategory", heritage.getSubcategory());
        item.put("dynasty", heritage.getDynasty());
        item.put("region", heritage.getRegion());
        item.put("level", heritage.getLevel());
        item.put("introduction", heritage.getIntroduction());
        item.put("description", heritage.getIntroduction());
        item.put("history", heritage.getHistory());
        item.put("inheritanceValue", heritage.getInheritanceValue());
        item.put("representativeInheritor", heritage.getRepresentativeInheritor());
        item.put("images", imageList);
        item.put("imageList", imageList);
        item.put("coverImage", imageList.isEmpty() ? null : imageList.get(0));
        item.put("videoUrl", heritage.getVideoUrl());
        item.put("isFeatured", heritage.getIsFeatured());
        item.put("viewCount", safeInt(heritage.getViewCount()));
        item.put("published", publishedIds.contains(heritage.getId()));
        return item;
    }

    private Map<String, Object> workToAdminMap(DiscoverPost post, List<Long> publishedIds) {
        Map<String, Object> item = new HashMap<>();
        List<String> imageList = parseStringList(post.getImages());
        item.put("id", post.getId());
        item.put("title", post.getTitle());
        item.put("category", post.getCategory());
        item.put("sourceLang", post.getSourceLang());
        item.put("description", post.getContent());
        item.put("content", post.getContent());
        item.put("coverImage", imageList.isEmpty() ? null : imageList.get(0));
        item.put("images", imageList);
        item.put("tags", parseStringList(post.getTags()));
        item.put("viewCount", safeInt(post.getViewCount()));
        item.put("collectCount", safeInt(post.getCollectCount()));
        item.put("commentCount", safeInt(post.getCommentCount()));
        item.put("heat", calculatePostHeat(post));
        item.put("createTime", post.getCreateTime());
        item.put("auditStatus", post.getAuditStatus());
        item.put("published", publishedIds.contains(post.getId()));
        userRepository.findById(post.getUserId()).ifPresent(user -> item.put("authorName", user.getNickname() != null ? user.getNickname() : user.getUsername()));
        return item;
    }

    private List<Long> extractActivityIds(List<Activity> activities) {
        List<Long> ids = new ArrayList<>();
        for (Activity activity : activities) if (activity.getId() != null) ids.add(activity.getId());
        return ids;
    }

    private List<Long> extractHeritageIds(List<IntangibleCulturalHeritage> heritages) {
        List<Long> ids = new ArrayList<>();
        for (IntangibleCulturalHeritage heritage : heritages) if (heritage.getId() != null) ids.add(heritage.getId());
        return ids;
    }

    private List<Long> normalizeIdList(List<Long> ids) {
        List<Long> result = new ArrayList<>();
        if (ids == null) return result;
        for (Long id : ids) if (id != null && !result.contains(id)) result.add(id);
        return result;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("写入JSON失败");
        }
    }

    private String writeStringList(Object value) {
        List<String> items = normalizeStringList(value);
        if (items.isEmpty()) return null;
        return writeJson(items);
    }

    private List<String> parseStringList(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();
        try {
            if (raw.trim().startsWith("[")) return normalizeStringList(objectMapper.readValue(raw, new TypeReference<List<String>>() { }));
        } catch (Exception ignored) {
        }
        return normalizeStringList(raw);
    }

    private List<String> normalizeStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value == null) return result;
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                String text = normalizeText(item);
                if (text != null) result.add(text);
            }
            return result;
        }
        String text = normalizeText(value);
        if (text == null) return result;
        String[] parts = text.replace("\r\n", "\n").contains("\n") ? text.replace("\r\n", "\n").split("\\n") : text.split(",");
        for (String part : parts) {
            String item = part == null ? null : part.trim();
            if (item != null && !item.isEmpty()) result.add(item);
        }
        if (result.isEmpty()) result.add(text);
        return result;
    }

    private String normalizeText(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Date parseDateValue(Object value) {
        if (value == null) return null;
        if (value instanceof Date) return (Date) value;
        if (value instanceof Number) return new Date(((Number) value).longValue());
        String text = normalizeText(value);
        if (text == null) return null;
        String[] patterns = new String[] { "yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(pattern);
                format.setLenient(false);
                return format.parse(text);
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("无效的日期值");
    }

    private Integer toNullableInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        String text = value.toString().trim();
        if (text.isEmpty()) return null;
        return Integer.parseInt(text);
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer toFlag(Object value) {
        if (value == null) return 0;
        if (value instanceof Boolean) return Boolean.TRUE.equals(value) ? 1 : 0;
        if (value instanceof Number) return ((Number) value).intValue() > 0 ? 1 : 0;
        String text = value.toString().trim();
        return ("1".equals(text) || "true".equalsIgnoreCase(text)) ? 1 : 0;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (Exception e) { return null; }
    }

    private Integer safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer safeInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (Exception e) { return 0; }
    }

    private int calculatePostHeat(DiscoverPost post) {
        return safeInt(post.getViewCount()) + safeInt(post.getCollectCount()) + safeInt(post.getCommentCount());
    }
}












