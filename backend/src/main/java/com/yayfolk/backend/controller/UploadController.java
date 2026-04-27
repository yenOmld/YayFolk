package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.service.UserService;
import com.yayfolk.backend.utils.QiniuOssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final QiniuOssUtil ossUtil;
    private final UserService userService;

    public UploadController(QiniuOssUtil ossUtil, UserService userService) {
        this.ossUtil = ossUtil;
        this.userService = userService;
    }

    @PostMapping("/avatar")
    public ResponseDto uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "请选择要上传的图片");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseDto.error(400, "只能上传图片文件");
            }

            long maxSize = 5 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "图片大小不能超过5MB");
            }

            // 获取用户信息
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            User user = userService.findByUsername(username);
            Long userId = user.getId();

            String url = ossUtil.uploadAvatar(file, userId);
            log.info("头像上传成功: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return ResponseDto.error(500, "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/post-image")
    public ResponseDto uploadPostImage(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "postId", required = false) Long postId,
                                        @RequestParam(value = "index", required = false) Integer index) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "请选择要上传的图片");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseDto.error(400, "只能上传图片文件");
            }

            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "图片大小不能超过10MB");
            }

            String url;
            if (postId != null && index != null) {
                // 使用postId和index上传，按顺序命名
                url = ossUtil.uploadPostImage(file, postId, index);
            } else {
                // 兼容逻辑
                url = ossUtil.uploadFile(file, "posts");
            }
            log.info("帖子图片上传成功: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("帖子图片上传失败", e);
            return ResponseDto.error(500, "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/image")
    public ResponseDto uploadImage(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "folder", defaultValue = "images") String folder) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "请选择要上传的图片");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseDto.error(400, "只能上传图片文件");
            }

            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "图片大小不能超过10MB");
            }

            String url = ossUtil.uploadFile(file, folder);
            log.info("图片上传成功: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            return ResponseDto.error(500, "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/homepage/image")
    public ResponseDto uploadHomepageImage(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "type", required = false) String type,
                                           HttpServletRequest request) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "请选择要上传的图片");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseDto.error(400, "只能上传图片文件");
            }

            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "图片大小不能超过10MB");
            }

            // 获取用户信息
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "未授权，请先登录");
            }
            String username = usernameObj.toString();
            User user = userService.findByUsername(username);
            Long userId = user.getId();

            String url = ossUtil.uploadHomepageImage(file, userId, type);
            log.info("个人主页图片上传成功: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("个人主页图片上传失败", e);
            return ResponseDto.error(500, "上传失败: " + e.getMessage());
        }
    }
    @PostMapping("/media")
    public ResponseDto uploadMedia(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "folder", defaultValue = "media") String folder) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "请选择要上传的文件");
            }

            String contentType = file.getContentType();
            boolean isImage = contentType != null && contentType.startsWith("image/");
            boolean isVideo = contentType != null && contentType.startsWith("video/");
            if (!isImage && !isVideo) {
                return ResponseDto.error(400, "只能上传图片或视频文件");
            }

            long maxSize = isVideo ? 100L * 1024 * 1024 : 10L * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, isVideo ? "视频大小不能超过100MB" : "图片大小不能超过10MB");
            }

            String url = ossUtil.uploadFile(file, folder);
            log.info("{}上传成功: {}", isVideo ? "视频" : "图片", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("媒体文件上传失败", e);
            return ResponseDto.error(500, "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/activity/image")
    public ResponseDto uploadActivityImage(@RequestParam("file") MultipartFile file,
                                          @RequestParam("activityId") Long activityId,
                                          @RequestParam("index") Integer index) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "请选择要上传的图片");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseDto.error(400, "只能上传图片文件");
            }

            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "图片大小不能超过10MB");
            }

            String url = ossUtil.uploadActivityImage(file, activityId, index);
            log.info("活动图片上传成功: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("活动图片上传失败", e);
            return ResponseDto.error(500, "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/activity/video")
    public ResponseDto uploadActivityVideo(@RequestParam("file") MultipartFile file,
                                          @RequestParam("activityId") Long activityId,
                                          @RequestParam("index") Integer index) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "请选择要上传的视频");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseDto.error(400, "只能上传视频文件");
            }

            long maxSize = 100 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "视频大小不能超过100MB");
            }

            String url = ossUtil.uploadActivityVideo(file, activityId, index);
            log.info("活动视频上传成功: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("活动视频上传失败", e);
            return ResponseDto.error(500, "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/model")
    public ResponseDto uploadModel(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "activityId", required = false) String activityIdStr,
                                   @RequestParam(value = "folder", defaultValue = "models") String folder) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "请选择要上传的文件");
            }

            String originalFilename = file.getOriginalFilename();
            log.info("VR上传 - activityIdStr: {}, folder: {}, originalFilename: {}", activityIdStr, folder, originalFilename);

            if (originalFilename == null ||
                (!originalFilename.toLowerCase().endsWith(".glb") &&
                 !originalFilename.toLowerCase().endsWith(".gltf"))) {
                return ResponseDto.error(400, "只能上传 .glb 或 .gltf 格式的3D模型文件");
            }

            long maxSize = 100 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "模型文件大小不能超过100MB");
            }

            Long activityId = null;
            if (activityIdStr != null && !activityIdStr.isEmpty()) {
                try {
                    activityId = Long.parseLong(activityIdStr);
                } catch (NumberFormatException e) {
                    log.warn("Invalid activityId format: {}", activityIdStr);
                }
            }

            String url;
            if (activityId != null) {
                url = ossUtil.uploadActivityVR(file, activityId);
            } else {
                url = ossUtil.uploadFile(file, folder);
            }
            log.info("3D模型上传成功: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("3D模型上传失败", e);
            return ResponseDto.error(500, "上传失败: " + e.getMessage());
        }
    }
}
