package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public User register(String username, String password, String nickname, String phone, String email, String country, String langCode, String regionCode) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查手机号是否已存在
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new RuntimeException("手机号已存在");
        }

        // 检查邮箱是否已存在
        if (email != null && userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setEmail(email);
        user.setCountry(country);
        user.setLangCode(langCode);
        user.setRegionCode(regionCode);

        // 如果没有提供昵称，生成默认昵称
        if (nickname == null || nickname.isEmpty()) {
            user.setNickname("用户" + new Random().nextInt(10000));
        } else {
            user.setNickname(nickname);
        }

        // 设置默认头像
        user.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + username);

        User savedUser = userRepository.save(user);

        return savedUser;
    }

    private void ensureUserEnabled(User user) {
        if (user.getStatus() != null && user.getStatus() == 0) {
            String reason = user.getBanReason();
            if (reason != null && !reason.trim().isEmpty()) {
                throw new RuntimeException("账号已被封禁，原因：" + reason.trim());
            }
            throw new RuntimeException("账号已被封禁");
        }
    }

    public User login(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (!userOptional.isPresent()) {
            throw new RuntimeException("用户名或密码错误");
        }

        User user = userOptional.get();
        ensureUserEnabled(user);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginTime(new java.util.Date());
        userRepository.save(user);

        return user;
    }

    public User findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        ensureUserEnabled(user);
        return user;
    }

    public void resetPassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User findByGithubId(String githubId) {
        return userRepository.findByGithubId(githubId)
                .orElse(null);
    }

    public void save(User user) {
        try {
            // 数据验证
            if (user.getNickname() != null && user.getNickname().length() > 50) {
                throw new RuntimeException("昵称不能超过 50 个字符");
            }
            if (user.getLangCode() != null && user.getLangCode().length() > 10) {
                throw new RuntimeException("语言代码不能超过 10 个字符");
            }
            if (user.getRegionCode() != null && user.getRegionCode().length() > 10) {
                throw new RuntimeException("地区代码不能超过 10 个字符");
            }
            // Avatar 字段特殊处理：Base64 图片数据可能很长，使用 TEXT 类型存储
            if (user.getAvatar() != null && user.getAvatar().startsWith("data:image")) {
                System.out.println("保存 Base64 图片数据，长度：" + user.getAvatar().length());
                // Base64 图片数据允许较长，但建议不超过 16MB（MySQL TEXT 上限）
                if (user.getAvatar().length() > 16000000) {
                    throw new RuntimeException("图片数据过大，请压缩后上传（最大 16MB）");
                }
            } else if (user.getAvatar() != null && user.getAvatar().length() > 2000) {
                System.out.println("警告：Avatar URL 较长，" + user.getAvatar().length() + " 字符），建议使用图床服务");
            }

            userRepository.save(user);
            System.out.println("用户数据保存成功");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            System.err.println("保存用户时发生数据库完整性错误：" + e.getMessage());
            e.printStackTrace();
            String causeMsg = e.getRootCause() != null ? e.getRootCause().getMessage() : "未知原因";
            throw new RuntimeException("数据保存失败：" + causeMsg, e);
        } catch (org.hibernate.exception.DataException e) {
            System.err.println("Hibernate 数据异常：" + e.getMessage());
            e.printStackTrace();
            String causeMsg = e.getCause() != null ? e.getCause().getMessage() : "未知原因";
            throw new RuntimeException("数据值超出范围或格式错误：" + causeMsg, e);
        } catch (Exception e) {
            System.err.println("保存用户时发生异常：" + e.getClass().getName());
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "未知异常";
            throw new RuntimeException("保存失败：" + errorMsg, e);
        }
    }

    public User createOrUpdateUserFromGithub(String githubId, String username, String email, String name, String avatarUrl) {
        // 首先尝试通过 GitHub ID 查找用户
        User user = findByGithubId(githubId);

        if (user != null) {
            ensureUserEnabled(user);
            // 已存在 GitHub 绑定用户，更新信息
            user.setUsername(username);
            user.setEmail(email);
            if (name != null && !name.isEmpty()) {
                user.setNickname(name);
            }
            if (avatarUrl != null) {
                user.setAvatar(avatarUrl);
            }
            return userRepository.save(user);
        }

        // 如果 GitHub ID 不存在，检查邮箱是否已存在
        if (email != null) {
            Optional<User> existingUserOptional = userRepository.findByEmail(email);
            if (existingUserOptional.isPresent()) {
                // 邮箱已存在，将 GitHub ID 关联到该用户
                user = existingUserOptional.get();
                ensureUserEnabled(user);
                user.setGithubId(githubId);
                user.setUsername(username);
                if (name != null && !name.isEmpty()) {
                    user.setNickname(name);
                }
                if (avatarUrl != null) {
                    user.setAvatar(avatarUrl);
                }
                return userRepository.save(user);
            }
        }

        // 邮箱也不存在，创建新用户
        user = new User();
        user.setGithubId(githubId);
        user.setUsername(username);
        // 生成随机密码，因为 OAuth 登录不需要密码
        user.setPassword(passwordEncoder.encode("github_oauth_" + new Random().nextInt(10000)));
        user.setEmail(email);
        user.setNickname(name != null && !name.isEmpty() ? name : username);
        user.setAvatar(avatarUrl);
        user.setCountry("Unknown");
        user.setLangCode("en"); // GitHub 用户默认使用英文

        User savedUser = userRepository.save(user);

        return savedUser;
    }

    public void deleteAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        userRepository.delete(user);
    }

    public User findByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Transactional
    public String updateAvatar(String username, String newAvatarUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String oldAvatar = user.getAvatar();
        user.setAvatar(newAvatarUrl);
        userRepository.save(user);

        return oldAvatar;
    }

    @Transactional
    public void deleteAvatar(String avatarUrl) {
        // 如果是默认头像或外部头像，不需要删除
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return;
        }
        // 如果是默认头像，不删除
        if (avatarUrl.contains("dicebear.com")) {
            return;
        }
        // 如果是 Base64 数据，不删除
        if (avatarUrl.startsWith("data:image")) {
            return;
        }
        // OSS 头像会在外部调用 OssUtil.deleteFile 删除
    }
}
