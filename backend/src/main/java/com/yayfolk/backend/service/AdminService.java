package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.DiscoverPost;
import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.MerchantApplication;
import com.yayfolk.backend.entity.MerchantProfile;
import com.yayfolk.backend.entity.OfficialContent;
import com.yayfolk.backend.entity.PostReport;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.DiscoverPostRepository;
import com.yayfolk.backend.repository.MerchantApplicationRepository;
import com.yayfolk.backend.repository.MerchantProfileRepository;
import com.yayfolk.backend.repository.OfficialContentRepository;
import com.yayfolk.backend.repository.PostReportRepository;
import com.yayfolk.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantApplicationRepository applicationRepository;
    private final ActivityRepository activityRepository;
    private final DiscoverPostRepository postRepository;
    private final OfficialContentRepository officialContentRepository;
    private final PostReportRepository postReportRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        MerchantProfileRepository merchantProfileRepository,
                        MerchantApplicationRepository applicationRepository,
                        ActivityRepository activityRepository,
                        DiscoverPostRepository postRepository,
                        OfficialContentRepository officialContentRepository,
                        PostReportRepository postReportRepository) {
        this.userRepository = userRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.applicationRepository = applicationRepository;
        this.activityRepository = activityRepository;
        this.postRepository = postRepository;
        this.officialContentRepository = officialContentRepository;
        this.postReportRepository = postReportRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    private User requireAdmin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        if (!"admin".equals(user.getRole())) {
            throw new RuntimeException("Only admins can access this resource");
        }
        return user;
    }

    private User requireSuperAdmin(String username) {
        User user = requireAdmin(username);
        if (!isSuperAdmin(user)) {
            throw new RuntimeException("Only the super admin can manage administrator accounts");
        }
        return user;
    }

    private User requireAdminAccount(Long adminId) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Administrator does not exist"));
        if (!"admin".equals(user.getRole())) {
            throw new RuntimeException("Target user is not an administrator");
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
            throw new RuntimeException("驳回原因不能为空");
        }
        app.setAuditRemark(approve ? null : trimmedRemark);
        app.setAuditAdminId(admin.getId());
        app.setAuditTime(new Date());
        applicationRepository.save(app);

        User applicant = userRepository.findById(app.getUserId())
                .orElseThrow(() -> new RuntimeException("Applicant does not exist"));
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
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));

        activity.setAuditStatus(approve ? "approved" : "rejected");
        String trimmedRemark = remark == null ? null : remark.trim();
        if (!approve && (trimmedRemark == null || trimmedRemark.isEmpty())) {
            throw new RuntimeException("驳回原因不能为空");
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
                .orElseThrow(() -> new RuntimeException("Post does not exist"));
        String trimmedRemark = remark == null ? null : remark.trim();
        if (!pass && (trimmedRemark == null || trimmedRemark.isEmpty())) {
            throw new RuntimeException("驳回原因不能为空");
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
            throw new RuntimeException("Please select posts first");
        }
        String trimmedRemark = remark == null ? null : remark.trim();
        if (!pass && (trimmedRemark == null || trimmedRemark.isEmpty())) {
            throw new RuntimeException("Please provide a rejection reason");
        }

        List<Long> successIds = new ArrayList<Long>();
        List<Map<String, Object>> failures = new ArrayList<Map<String, Object>>();
        for (Long postId : postIds) {
            if (postId == null) {
                continue;
            }
            try {
                DiscoverPost post = postRepository.findById(postId)
                        .orElseThrow(() -> new RuntimeException("Post does not exist"));
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
            log.warn("Failed to query pending report counts; fallback to zero counts", e);
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
            log.warn("Failed to resolve pending reports for post {}", postId, e);
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

    public void banUser(String adminUsername, Long userId) {
        requireAdmin(adminUsername);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        if ("admin".equalsIgnoreCase(user.getRole()) || isSuperAdmin(user)) {
            throw new RuntimeException("管理员账号不能在用户管理中封禁");
        }
        user.setStatus(0);
        userRepository.save(user);
    }

    public void unbanUser(String adminUsername, Long userId) {
        requireAdmin(adminUsername);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        if ("admin".equalsIgnoreCase(user.getRole()) || isSuperAdmin(user)) {
            throw new RuntimeException("管理员账号不能在用户管理中解封");
        }
        user.setStatus(1);
        userRepository.save(user);
    }

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
                .orElseThrow(() -> new RuntimeException("Official content does not exist"));
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

        String username = getRequiredString(data, "username", "Administrator username is required");
        String password = getRequiredString(data, "password", "Administrator password is required");
        String confirmPassword = getRequiredString(data, "confirmPassword", "Please confirm the administrator password");
        String nickname = getOptionalString(data, "nickname");
        String email = getOptionalString(data, "email");

        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("The two passwords do not match");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
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
            throw new RuntimeException("New password cannot be empty");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("The two passwords do not match");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
        return userToMap(admin);
    }

    public void deleteAdmin(String adminUsername, Long adminId) {
        User operator = requireSuperAdmin(adminUsername);
        User admin = requireAdminAccount(adminId);

        if (operator.getId().equals(admin.getId())) {
            throw new RuntimeException("The super admin cannot delete the current logged-in account");
        }
        if (isSuperAdmin(admin)) {
            throw new RuntimeException("The super admin account cannot be deleted");
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
        m.put("isSuperAdmin", u.getIsSuperAdmin());
        m.put("createTime", u.getCreateTime());
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
        m.put("id", activity.getId());
        m.put("merchantId", activity.getMerchantId());
        m.put("merchantProfileId", activity.getMerchantProfileId());
        m.put("title", activity.getTitle());
        m.put("subtitle", activity.getSubtitle());
        m.put("coverImage", activity.getCoverImage());
        m.put("heritageType", activity.getHeritageType());
        m.put("activityType", activity.getActivityType());
        m.put("startTime", activity.getStartTime());
        m.put("endTime", activity.getEndTime());
        m.put("price", activity.getPrice());
        m.put("maxParticipants", activity.getMaxParticipants());
        m.put("currentParticipants", activity.getCurrentParticipants());
        m.put("locationCity", activity.getLocationCity());
        m.put("locationDetail", activity.getLocationDetail());
        m.put("status", activity.getStatus());
        m.put("auditStatus", activity.getAuditStatus());
        m.put("auditRemark", activity.getAuditRemark());
        m.put("content", activity.getContent());
        m.put("createTime", activity.getCreateTime());
        userRepository.findById(activity.getMerchantId()).ifPresent(user -> {
            m.put("merchantName", user.getShopName() != null ? user.getShopName() : user.getNickname());
            m.put("merchantAvatar", user.getAvatar());
            m.put("merchantUsername", user.getUsername());
        });
        return m;
    }
}


