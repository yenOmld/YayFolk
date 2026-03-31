package com.yayfolk.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.DiscoverPostRepository;
import com.yayfolk.backend.service.UserCenterService;
import com.yayfolk.backend.service.UserService;
import com.yayfolk.backend.util.OssUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/profile")
    public ResponseDto getUserProfile(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            User user = userService.findByUsername(username);
            return ResponseDto.success(user);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

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
            String username = requireUsername(request);
            User user = userService.findByUsername(username);
            return ResponseDto.success(userCenterService.getHomepageSettings(user));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/homepage-settings")
    public ResponseDto updateHomepageSettings(@RequestBody Map<String, Object> updateData, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            User user = userService.findByUsername(username);

            if (updateData.containsKey("bio")) {
                String bio = valueAsString(updateData.get("bio"));
                if (bio != null && bio.length() > 200) {
                    return ResponseDto.error(400, "Bio cannot exceed 200 characters");
                }
                user.setBio(bio);
            }
            if (updateData.containsKey("signature")) {
                String signature = valueAsString(updateData.get("signature"));
                if (signature != null && signature.length() > 60) {
                    return ResponseDto.error(400, "Signature cannot exceed 60 characters");
                }
                user.setSignature(signature);
            }
            if (updateData.containsKey("location")) {
                String location = valueAsString(updateData.get("location"));
                if (location != null && location.length() > 100) {
                    return ResponseDto.error(400, "Location cannot exceed 100 characters");
                }
                user.setLocation(location);
            }
            if (updateData.containsKey("shopName")) {
                String shopName = valueAsString(updateData.get("shopName"));
                if (shopName != null && shopName.length() > 100) {
                    return ResponseDto.error(400, "Shop name cannot exceed 100 characters");
                }
                user.setShopName(shopName);
            }
            if (updateData.containsKey("shopIntro")) {
                String shopIntro = valueAsString(updateData.get("shopIntro"));
                if (shopIntro != null && shopIntro.length() > 500) {
                    return ResponseDto.error(400, "Shop intro cannot exceed 500 characters");
                }
                user.setShopIntro(shopIntro);
            }
            if (updateData.containsKey("shopCover")) {
                String shopCover = valueAsString(updateData.get("shopCover"));
                if (shopCover != null && shopCover.length() > 255) {
                    return ResponseDto.error(400, "Shop cover is too long");
                }
                user.setShopCover(shopCover);
                if (!updateData.containsKey("coverPhoto") && !StringUtils.hasText(user.getCoverPhoto())) {
                    user.setCoverPhoto(shopCover);
                }
            }
            if (updateData.containsKey("coverPhoto")) {
                String coverPhoto = valueAsString(updateData.get("coverPhoto"));
                if (coverPhoto != null && coverPhoto.length() > 255) {
                    return ResponseDto.error(400, "Cover photo is too long");
                }
                user.setCoverPhoto(coverPhoto);
            }
            if (updateData.containsKey("collectionVisibility")) {
                user.setCollectionVisibility(normalizeVisibility(valueAsString(updateData.get("collectionVisibility"))));
            }

            userService.save(user);
            return ResponseDto.success(userCenterService.getHomepageSettings(user));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/{userId}/followers")
    public ResponseDto getFollowers(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(userCenterService.getFollowers(username, userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/{userId}/following")
    public ResponseDto getFollowing(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(userCenterService.getFollowing(username, userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/{userId}/collected-by")
    public ResponseDto getCollectedBy(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(userCenterService.getCollectedBy(username, userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/follow/{userId}")
    public ResponseDto followUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            return ResponseDto.success(userCenterService.followUser(requireUsername(request), userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/follow/{userId}")
    public ResponseDto unfollowUser(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            return ResponseDto.success(userCenterService.unfollowUser(requireUsername(request), userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/follow/{userId}/status")
    public ResponseDto getFollowStatus(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            return ResponseDto.success(userCenterService.getFollowStatus(requireUsername(request), userId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/visitor-records")
    public ResponseDto getVisitorRecords(HttpServletRequest request) {
        try {
            return ResponseDto.success(userCenterService.getVisitorRecords(requireUsername(request)));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseDto updateUserProfile(@RequestBody Map<String, Object> updateData, HttpServletRequest request) {
        try {
            String username = requireUsernameWithFallback(request);
            User user = userService.findByUsername(username);

            if (updateData.containsKey("nickname")) {
                String nickname = valueAsString(updateData.get("nickname"));
                if (nickname != null && nickname.length() > 50) {
                    return ResponseDto.error(400, "Nickname cannot exceed 50 characters");
                }
                user.setNickname(nickname);
            }
            if (updateData.containsKey("avatar")) {
                String avatar = valueAsString(updateData.get("avatar"));
                String oldAvatar = user.getAvatar();
                if (StringUtils.hasText(avatar) && !avatar.equals(oldAvatar) && StringUtils.hasText(oldAvatar) && ossUtil.isOssUrl(oldAvatar)) {
                    ossUtil.deleteFile(oldAvatar);
                }
                user.setAvatar(avatar);
            }
            if (updateData.containsKey("bio")) {
                String bio = valueAsString(updateData.get("bio"));
                if (bio != null && bio.length() > 200) {
                    return ResponseDto.error(400, "Bio cannot exceed 200 characters");
                }
                user.setBio(bio);
            }
            if (updateData.containsKey("signature")) {
                String signature = valueAsString(updateData.get("signature"));
                if (signature != null && signature.length() > 60) {
                    return ResponseDto.error(400, "Signature cannot exceed 60 characters");
                }
                user.setSignature(signature);
            }
            if (updateData.containsKey("langCode")) {
                String langCode = valueAsString(updateData.get("langCode"));
                if (langCode != null && langCode.length() > 10) {
                    return ResponseDto.error(400, "Invalid language code");
                }
                user.setLangCode(langCode);
            }
            if (updateData.containsKey("regionCode")) {
                String regionCode = valueAsString(updateData.get("regionCode"));
                if (regionCode != null && regionCode.length() > 10) {
                    return ResponseDto.error(400, "Invalid region code");
                }
                user.setRegionCode(regionCode);
            }

            userService.save(user);
            return ResponseDto.success(user);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/language")
    public ResponseDto updateLanguage(@RequestBody Map<String, String> languageData, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
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

    @PutMapping("/region")
    public ResponseDto updateRegion(@RequestBody Map<String, String> regionData, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
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

    @PostMapping("/change-password")
    public ResponseDto changePassword(@RequestBody Map<String, String> passwordData, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            User user = userService.findByUsername(username);

            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            String confirmPassword = passwordData.get("confirmPassword");

            if (!StringUtils.hasText(oldPassword)) {
                return ResponseDto.error(400, "Please enter your current password");
            }

            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ResponseDto.error(400, "Current password is incorrect");
            }
            if (!StringUtils.hasText(newPassword)) {
                return ResponseDto.error(400, "Please enter a new password");
            }
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                return ResponseDto.error(400, "New password must be different from the current password");
            }
            if (newPassword.length() < 8 || newPassword.length() > 20) {
                return ResponseDto.error(400, "Password length must be between 8 and 20 characters");
            }
            if (!newPassword.matches(".*[a-zA-Z].*")) {
                return ResponseDto.error(400, "Password must contain letters");
            }
            if (!newPassword.matches(".*[0-9].*")) {
                return ResponseDto.error(400, "Password must contain numbers");
            }
            if (!StringUtils.hasText(confirmPassword)) {
                return ResponseDto.error(400, "Please confirm the new password");
            }
            if (!newPassword.equals(confirmPassword)) {
                return ResponseDto.error(400, "Passwords do not match");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userService.save(user);
            return ResponseDto.success("Password updated successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/achievements")
    public ResponseDto getAchievements(HttpServletRequest request) {
        try {
            return ResponseDto.success(userCenterService.getAchievements(requireUsername(request)));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/account")
    public ResponseDto deleteAccount(HttpServletRequest request) {
        try {
            String username = requireUsernameWithFallback(request);
            User user = userService.findByUsername(username);
            Long userId = user.getId();

            String avatar = user.getAvatar();
            if (StringUtils.hasText(avatar) && ossUtil.isOssUrl(avatar)) {
                ossUtil.deleteFile(avatar);
            }

            List<com.yayfolk.backend.entity.DiscoverPost> posts = postRepository.findByUserId(userId);
            for (com.yayfolk.backend.entity.DiscoverPost post : posts) {
                try {
                    List<String> images = objectMapper.readValue(post.getImages(), new TypeReference<List<String>>() {});
                    for (String image : images) {
                        if (ossUtil.isOssUrl(image)) {
                            ossUtil.deleteFile(image);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            userService.deleteAccount(username);
            return ResponseDto.success("Account deleted successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    private String requireUsername(HttpServletRequest request) {
        Object usernameObj = request.getAttribute("username");
        if (usernameObj == null) {
            throw new RuntimeException("Please log in first");
        }
        return usernameObj.toString();
    }

    private String requireUsernameWithFallback(HttpServletRequest request) {
        Object usernameObj = request.getAttribute("username");
        if (usernameObj == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                usernameObj = auth.getName();
            }
        }
        if (usernameObj == null) {
            throw new RuntimeException("Please log in first");
        }
        return usernameObj.toString();
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String normalizeVisibility(String visibility) {
        return "private".equalsIgnoreCase(visibility) ? "private" : "public";
    }
}
