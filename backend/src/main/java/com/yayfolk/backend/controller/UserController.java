package com.yayfolk.backend.controller;

import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.service.UserCenterService;
import com.yayfolk.backend.service.UserService;
import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.util.OssUtil;
import com.yayfolk.backend.repository.DiscoverPostRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserCenterService userCenterService;
    private final OssUtil ossUtil;
    private final DiscoverPostRepository postRepository;
    private final ObjectMapper objectMapper;

    public UserController(UserService userService,
                          UserCenterService userCenterService,
                          OssUtil ossUtil,
                          DiscoverPostRepository postRepository,
                          ObjectMapper objectMapper) {
        this.userService = userService;
        this.userCenterService = userCenterService;
        this.ossUtil = ossUtil;
        this.postRepository = postRepository;
        this.objectMapper = objectMapper;
    }

    // 获取用户信息
    @GetMapping("/profile")
    public ResponseDto getUserProfile(HttpServletRequest request) {
        try {
            // 从请求中获取用户名（这里简化处理，实际应该从JWT中解析）
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            User user = userService.findByUsername(username);
            return ResponseDto.success(user);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    // 更新用户信息
    @GetMapping("/homepage/{userId}")
    public ResponseDto getUserHomepage(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            String viewerUsername = usernameObj == null ? null : usernameObj.toString();
            return ResponseDto.success(userCenterService.getUserHomepage(viewerUsername, userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/homepage-settings")
    public ResponseDto getHomepageSettings(HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            User user = userService.findByUsername(username);
            return ResponseDto.success(userCenterService.getHomepageSettings(user));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/homepage-settings")
    public ResponseDto updateHomepageSettings(@RequestBody Map<String, Object> updateData, HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            User user = userService.findByUsername(username);

            if (updateData.containsKey("bio")) {
                String bio = (String) updateData.get("bio");
                if (bio != null && bio.length() > 200) {
                    return ResponseDto.error(400, "主页简介不能超过 200 个字符");
                }
                user.setBio(bio);
            }
            if (updateData.containsKey("location")) {
                String location = (String) updateData.get("location");
                if (location != null && location.length() > 100) {
                    return ResponseDto.error(400, "主页地区不能超过 100 个字符");
                }
                user.setLocation(location);
            }
            if (updateData.containsKey("shopName")) {
                String shopName = (String) updateData.get("shopName");
                if (shopName != null && shopName.length() > 100) {
                    return ResponseDto.error(400, "主页标题不能超过 100 个字符");
                }
                user.setShopName(shopName);
            }
            if (updateData.containsKey("shopIntro")) {
                String shopIntro = (String) updateData.get("shopIntro");
                if (shopIntro != null && shopIntro.length() > 500) {
                    return ResponseDto.error(400, "主页详情不能超过 500 个字符");
                }
                user.setShopIntro(shopIntro);
            }
            if (updateData.containsKey("shopCover")) {
                String shopCover = (String) updateData.get("shopCover");
                if (shopCover != null && shopCover.length() > 255) {
                    return ResponseDto.error(400, "主页封面地址过长");
                }
                user.setShopCover(shopCover);
            }

            userService.save(user);
            return ResponseDto.success(userCenterService.getHomepageSettings(user));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/follow/{userId}")
    public ResponseDto followUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            return ResponseDto.success(userCenterService.followUser(usernameObj.toString(), userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/follow/{userId}")
    public ResponseDto unfollowUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            return ResponseDto.success(userCenterService.unfollowUser(usernameObj.toString(), userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/follow/{userId}/status")
    public ResponseDto getFollowStatus(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            return ResponseDto.success(userCenterService.getFollowStatus(usernameObj.toString(), userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/visitor-records")
    public ResponseDto getVisitorRecords(HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            return ResponseDto.success(userCenterService.getVisitorRecords(usernameObj.toString()));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseDto updateUserProfile(@RequestBody Map<String, Object> updateData, HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                    usernameObj = auth.getName();
                }
            }
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            System.out.println("更新用户资料 - 用户名：" + username + ", 数据：" + updateData);
            User user = userService.findByUsername(username);

            // 更新字段
            if (updateData.containsKey("nickname")) {
                String nickname = (String) updateData.get("nickname");
                if (nickname != null && nickname.length() > 50) {
                    return ResponseDto.error(400, "昵称不能超过 50 个字符");
                }
                user.setNickname(nickname);
            }
            if (updateData.containsKey("avatar")) {
                String avatar = (String) updateData.get("avatar");
                String oldAvatar = user.getAvatar();
                
                // 如果新头像和旧头像不同，且旧头像是OSS图片，删除旧头像
                if (avatar != null && !avatar.equals(oldAvatar)) {
                    if (oldAvatar != null && ossUtil.isOssUrl(oldAvatar)) {
                        ossUtil.deleteFile(oldAvatar);
                        System.out.println("删除旧头像：" + oldAvatar);
                    }
                }
                
                // 处理 Base64 图片数据（向后兼容）
                if (avatar != null && avatar.startsWith("data:image")) {
                    System.out.println("检测到 Base64 图片数据，长度：" + avatar.length());
                    // 如果图片太大，给出警告但仍然保存
                    if (avatar.length() > 50000) {
                        System.out.println("警告：图片数据过大（" + avatar.length() + " 字节），可能导致数据库性能问题");
                        // 建议前端压缩图片或上传到图床
                    }
                } else if (avatar != null && avatar.length() > 1000) {
                    System.out.println("Avatar URL：" + avatar);
                }
                user.setAvatar(avatar);
            }
            if (updateData.containsKey("langCode")) {
                String langCode = (String) updateData.get("langCode");
                if (langCode != null && langCode.length() > 10) {
                    return ResponseDto.error(400, "语言代码格式不正确");
                }
                user.setLangCode(langCode);
            }
            if (updateData.containsKey("regionCode")) {
                String regionCode = (String) updateData.get("regionCode");
                if (regionCode != null && regionCode.length() > 10) {
                    return ResponseDto.error(400, "地区代码格式不正确");
                }
                user.setRegionCode(regionCode);
            }

            // 保存更新
            System.out.println("保存用户数据：" + user);
            userService.save(user);
            System.out.println("用户资料更新成功");
            return ResponseDto.success(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            System.err.println("数据库完整性 violation: " + e.getMessage());
            e.printStackTrace();
            String causeMsg = e.getRootCause() != null ? e.getRootCause().getMessage() : "未知原因";
            return ResponseDto.error(400, "数据格式错误：" + causeMsg);
        } catch (org.hibernate.exception.DataException e) {
            System.err.println("Hibernate 数据异常：" + e.getMessage());
            e.printStackTrace();
            String causeMsg = e.getCause() != null ? e.getCause().getMessage() : "未知原因";
            return ResponseDto.error(400, "数据值超出范围或格式错误：" + causeMsg);
        } catch (ClassCastException e) {
            System.err.println("类型转换错误：" + e.getMessage());
            e.printStackTrace();
            return ResponseDto.error(400, "数据类型错误，请检查输入格式");
        } catch (IllegalArgumentException e) {
            System.err.println("参数错误：" + e.getMessage());
            e.printStackTrace();
            return ResponseDto.error(400, "参数错误：" + (e.getMessage() != null ? e.getMessage() : "无效的参数"));
        } catch (Exception e) {
            System.err.println("更新失败：" + e.getClass().getName());
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "未知错误，请查看服务器日志";
            }
            return ResponseDto.error(400, "更新失败：" + errorMsg);
        }
    }

    // 更新语言偏好
    @PutMapping("/language")
    public ResponseDto updateLanguage(@RequestBody Map<String, String> languageData, HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            User user = userService.findByUsername(username);

            if (languageData.containsKey("langCode")) {
                user.setLangCode(languageData.get("langCode"));
            }

            userService.save(user);
            return ResponseDto.success(user);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    // 更新地区码（登录时调用）
    @PutMapping("/region")
    public ResponseDto updateRegion(@RequestBody Map<String, String> regionData, HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            User user = userService.findByUsername(username);

            if (regionData.containsKey("regionCode")) {
                user.setRegionCode(regionData.get("regionCode"));
            }
            if (regionData.containsKey("country")) {
                user.setCountry(regionData.get("country"));
            }

            userService.save(user);
            return ResponseDto.success(user);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    // 修改密码
    @PostMapping("/change-password")
    public ResponseDto changePassword(@RequestBody Map<String, String> passwordData, HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            User user = userService.findByUsername(username);

            // 获取密码数据
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            String confirmPassword = passwordData.get("confirmPassword");

            // 验证原密码
            if (oldPassword == null || oldPassword.isEmpty()) {
                return ResponseDto.error(400, "请输入原密码");
            }

            // 验证原密码是否正确（使用 BCrypt 匹配）
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ResponseDto.error(400, "原密码错误");
            }

            // 验证新密码
            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseDto.error(400, "请输入新密码");
            }

            // 验证新密码不能与原密码相同
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                return ResponseDto.error(400, "新密码不能与原密码相同");
            }

            // 验证新密码格式
            if (newPassword.length() < 8 || newPassword.length() > 20) {
                return ResponseDto.error(400, "新密码长度应为8-20位");
            }
            if (!newPassword.matches(".*[a-zA-Z].*")) {
                return ResponseDto.error(400, "新密码必须包含字母");
            }
            if (!newPassword.matches(".*[0-9].*")) {
                return ResponseDto.error(400, "新密码必须包含数字");
            }

            // 验证确认密码
            if (confirmPassword == null || confirmPassword.isEmpty()) {
                return ResponseDto.error(400, "请再次输入新密码");
            }
            if (!newPassword.equals(confirmPassword)) {
                return ResponseDto.error(400, "两次输入的密码不一致");
            }

            // 加密新密码
            user.setPassword(passwordEncoder.encode(newPassword));

            // 保存更新
            userService.save(user);

            return ResponseDto.success("密码修改成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/achievements")
    public ResponseDto getAchievements(HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            return ResponseDto.success(userCenterService.getAchievements(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/account")
    public ResponseDto deleteAccount(HttpServletRequest request) {
        try {
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                    usernameObj = auth.getName();
                }
            }
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            
            // 获取用户信息
            User user = userService.findByUsername(username);
            Long userId = user.getId();
            
            // 删除用户头像
            String avatar = user.getAvatar();
            if (avatar != null && ossUtil.isOssUrl(avatar)) {
                ossUtil.deleteFile(avatar);
                System.out.println("删除用户头像：" + avatar);
            }
            
            // 删除用户所有帖子的图片
            List<com.yayfolk.backend.entity.DiscoverPost> posts = postRepository.findByUserId(userId);
            for (com.yayfolk.backend.entity.DiscoverPost post : posts) {
                try {
                    List<String> images = objectMapper.readValue(post.getImages(), new TypeReference<List<String>>() {});
                    for (String image : images) {
                        if (ossUtil.isOssUrl(image)) {
                            ossUtil.deleteFile(image);
                            System.out.println("删除帖子图片：" + image);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("解析帖子图片失败：" + post.getId());
                }
            }
            
            userService.deleteAccount(username);
            return ResponseDto.success("账号已注销");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
}
