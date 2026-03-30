package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.service.UserService;
import com.yayfolk.backend.util.OssUtil;
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

    private final OssUtil ossUtil;
    private final UserService userService;

    public UploadController(OssUtil ossUtil, UserService userService) {
        this.ossUtil = ossUtil;
        this.userService = userService;
    }

    @PostMapping("/avatar")
    public ResponseDto uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "璇烽€夋嫨瑕佷笂浼犵殑鍥剧墖");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseDto.error(400, "鍙兘涓婁紶鍥剧墖鏂囦欢");
            }

            long maxSize = 5 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "鍥剧墖澶у皬涓嶈兘瓒呰繃5MB");
            }

            // 鑾峰彇鐢ㄦ埛淇℃伅
            Object usernameObj = request.getAttribute("username");
            if (usernameObj == null) {
                return ResponseDto.error(401, "鏈巿鏉冿紝璇峰厛鐧诲綍");
            }
            String username = usernameObj.toString();
            User user = userService.findByUsername(username);
            Long userId = user.getId();

            String url = ossUtil.uploadAvatar(file, userId);
            log.info("澶村儚涓婁紶鎴愬姛: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("澶村儚涓婁紶澶辫触", e);
            return ResponseDto.error(500, "涓婁紶澶辫触: " + e.getMessage());
        }
    }

    @PostMapping("/post-image")
    public ResponseDto uploadPostImage(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "postId", required = false) Long postId,
                                        @RequestParam(value = "index", required = false) Integer index) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "璇烽€夋嫨瑕佷笂浼犵殑鍥剧墖");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseDto.error(400, "鍙兘涓婁紶鍥剧墖鏂囦欢");
            }

            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "鍥剧墖澶у皬涓嶈兘瓒呰繃10MB");
            }

            String url;
            if (postId != null && index != null) {
                // 浣跨敤postId鍜宨ndex涓婁紶锛屾寜椤哄簭鍛藉悕
                url = ossUtil.uploadPostImage(file, postId, index);
            } else {
                // 鍏煎鏃ч€昏緫
                url = ossUtil.uploadFile(file, "posts");
            }
            log.info("甯栧瓙鍥剧墖涓婁紶鎴愬姛: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("甯栧瓙鍥剧墖涓婁紶澶辫触", e);
            return ResponseDto.error(500, "涓婁紶澶辫触: " + e.getMessage());
        }
    }

    @PostMapping("/image")
    public ResponseDto uploadImage(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "folder", defaultValue = "images") String folder) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "璇烽€夋嫨瑕佷笂浼犵殑鍥剧墖");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseDto.error(400, "鍙兘涓婁紶鍥剧墖鏂囦欢");
            }

            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, "鍥剧墖澶у皬涓嶈兘瓒呰繃10MB");
            }

            String url = ossUtil.uploadFile(file, folder);
            log.info("鍥剧墖涓婁紶鎴愬姛: {}", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("鍥剧墖涓婁紶澶辫触", e);
            return ResponseDto.error(500, "涓婁紶澶辫触: " + e.getMessage());
        }
    }
    @PostMapping("/media")
    public ResponseDto uploadMedia(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "folder", defaultValue = "media") String folder) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseDto.error(400, "璇烽€夋嫨瑕佷笂浼犵殑鏂囦欢");
            }

            String contentType = file.getContentType();
            boolean isImage = contentType != null && contentType.startsWith("image/");
            boolean isVideo = contentType != null && contentType.startsWith("video/");
            if (!isImage && !isVideo) {
                return ResponseDto.error(400, "鍙兘涓婁紶鍥剧墖鎴栬棰戞枃浠?");
            }

            long maxSize = isVideo ? 100L * 1024 * 1024 : 10L * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseDto.error(400, isVideo ? "瑙嗛澶у皬涓嶈兘瓒呰繃100MB" : "鍥剧墖澶у皬涓嶈兘瓒呰繃10MB");
            }

            String url = ossUtil.uploadFile(file, folder);
            log.info("{}涓婁紶鎴愬姛: {}", isVideo ? "瑙嗛" : "鍥剧墖", url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ResponseDto.success(result);
        } catch (Exception e) {
            log.error("濯掍綋鏂囦欢涓婁紶澶辫触", e);
            return ResponseDto.error(500, "涓婁紶澶辫触: " + e.getMessage());
        }
    }
}
