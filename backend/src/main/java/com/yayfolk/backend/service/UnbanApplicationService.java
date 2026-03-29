package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.entity.UserUnbanApplication;
import com.yayfolk.backend.repository.UserRepository;
import com.yayfolk.backend.repository.UserUnbanApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UnbanApplicationService {

    private final UserRepository userRepository;
    private final UserUnbanApplicationRepository userUnbanApplicationRepository;

    public UnbanApplicationService(UserRepository userRepository,
                                   UserUnbanApplicationRepository userUnbanApplicationRepository) {
        this.userRepository = userRepository;
        this.userUnbanApplicationRepository = userUnbanApplicationRepository;
    }

    public Map<String, Object> submitApplication(String account, String reason) {
        String trimmedAccount = account == null ? null : account.trim();
        if (trimmedAccount == null || trimmedAccount.isEmpty()) {
            throw new RuntimeException("账号不能为空");
        }

        String trimmedReason = reason == null ? null : reason.trim();
        if (trimmedReason == null || trimmedReason.isEmpty()) {
            throw new RuntimeException("请填写解封申请原因");
        }

        User user = findUserByAccount(trimmedAccount);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        if ("admin".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("管理员账号无需申请解封");
        }
        if (!Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException("该账号当前未被封禁");
        }
        if (userUnbanApplicationRepository.existsByUserIdAndStatus(user.getId(), "pending")) {
            throw new RuntimeException("你已提交解封申请，请等待管理员审核");
        }

        UserUnbanApplication application = new UserUnbanApplication();
        application.setUserId(user.getId());
        application.setApplyReason(trimmedReason);
        application.setStatus("pending");

        UserUnbanApplication saved = userUnbanApplicationRepository.save(application);
        return toMap(saved, user);
    }

    public Map<String, Object> getLatestApplication(String account) {
        String trimmedAccount = account == null ? null : account.trim();
        if (trimmedAccount == null || trimmedAccount.isEmpty()) {
            throw new RuntimeException("账号不能为空");
        }

        User user = findUserByAccount(trimmedAccount);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }

        Optional<UserUnbanApplication> latest = userUnbanApplicationRepository.findTopByUserIdOrderByCreateTimeDesc(user.getId());
        if (!latest.isPresent()) {
            return null;
        }
        return toMap(latest.get(), user);
    }

    private User findUserByAccount(String account) {
        Optional<User> byUsername = userRepository.findByUsername(account);
        if (byUsername.isPresent()) {
            return byUsername.get();
        }
        return userRepository.findByEmail(account).orElse(null);
    }

    private Map<String, Object> toMap(UserUnbanApplication application, User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", application.getId());
        map.put("userId", application.getUserId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("email", user.getEmail());
        map.put("applyReason", application.getApplyReason());
        map.put("status", application.getStatus());
        map.put("adminId", application.getAdminId());
        map.put("adminRemark", application.getAdminRemark());
        map.put("createTime", application.getCreateTime());
        map.put("handleTime", application.getHandleTime());
        return map;
    }
}
