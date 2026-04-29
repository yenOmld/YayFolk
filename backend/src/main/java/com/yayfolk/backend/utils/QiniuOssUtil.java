package com.yayfolk.backend.utils;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.yayfolk.backend.config.QiniuOssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QiniuOssUtil {

    private final QiniuOssProperties qiniuOssProperties;

    public String uploadFile(MultipartFile file, String folder) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // 1. 获取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 2. 构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());  // 使用华东-浙江2区，与 yayfolk bucket 同区域
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;  //指定分片上传版本
        UploadManager uploadManager = new UploadManager(cfg);

        // 3. 生成上传凭证，然后准备上传
        byte[] bytes = file.getBytes();
        String fileName = folder + "/" + UUID.randomUUID().toString() + suffix;

        try {
            byte[] uploadBytes = file.getBytes();
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(uploadBytes);
            Auth auth = Auth.create(qiniuOssProperties.getAccessKey(), qiniuOssProperties.getSecretKey());
            String upToken = auth.uploadToken(qiniuOssProperties.getBucketName());

            try {
                uploadManager.put(byteInputStream, fileName, upToken, null, null);
            } catch (QiniuException ex) {
                Response r = ex.response;
                log.error("七牛云ERROR:{}", r.toString());
                try {
                    log.error("七牛云ERROR:{}", r.bodyString());
                } catch (QiniuException ex2) {
                    ex2.printStackTrace();
                }
                throw new RuntimeException("文件上传失败", ex);
            }
        } catch (Exception ex) {
            log.error("文件上传失败", ex);
            throw new RuntimeException("文件上传失败", ex);
        }

        String fileUrl = String.format("%s/%s", qiniuOssProperties.getDomainName(), fileName);
        return fileUrl;
    }

    private byte[] compressImage(MultipartFile file, int maxSizeKB) throws IOException {
        byte[] originalData = file.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalData);
        BufferedImage image = ImageIO.read(inputStream);

        if (image == null) {
            log.warn("Invalid image file, using original data");
            return originalData;
        }

        int targetSize = maxSizeKB * 1024;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        String originalFilename = file.getOriginalFilename();
        String format = "jpg";
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (extension.equals("png") || extension.equals("gif") || extension.equals("webp")) {
                format = extension;
            }
        }

        try {
            float quality = 0.9f;
            while (quality > 0.1f) {
                outputStream.reset();

                javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName(format).next();
                javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();

                if (param.canWriteCompressed()) {
                    param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality);
                }

                writer.setOutput(javax.imageio.ImageIO.createImageOutputStream(outputStream));
                writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
                writer.dispose();

                if (outputStream.size() <= targetSize) {
                    break;
                }
                quality -= 0.1f;
            }

            if (outputStream.size() > targetSize) {
                outputStream.reset();
                int width = image.getWidth();
                int height = image.getHeight();
                int maxDimension = 300;

                if (width > maxDimension || height > maxDimension) {
                    double scale = Math.min((double) maxDimension / width, (double) maxDimension / height);
                    int newWidth = (int) (width * scale);
                    int newHeight = (int) (height * scale);

                    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = resizedImage.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
                    g2d.dispose();

                    javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName(format).next();
                    javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();

                    if (param.canWriteCompressed()) {
                        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(0.8f);
                    }

                    writer.setOutput(javax.imageio.ImageIO.createImageOutputStream(outputStream));
                    writer.write(null, new javax.imageio.IIOImage(resizedImage, null, null), param);
                    writer.dispose();
                }
            }
        } catch (Exception e) {
            log.warn("Image compression failed, using original data", e);
            outputStream.reset();
            outputStream.write(originalData);
        } finally {
            inputStream.close();
        }

        return outputStream.toByteArray();
    }

    public String uploadAvatar(MultipartFile file, Long userId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // 1. 压缩图片
        byte[] compressedData = compressImage(file, 20);

        // 2. 获取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = "avatars/" + userId + suffix;

        // 3. 构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());  // 使用华东-浙江2区，与 yayfolk bucket 同区域
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;  //指定分片上传版本
        UploadManager uploadManager = new UploadManager(cfg);

        // 4. 生成上传凭证，然后准备上传
        try {
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(compressedData);
            Auth auth = Auth.create(qiniuOssProperties.getAccessKey(), qiniuOssProperties.getSecretKey());
            String upToken = auth.uploadToken(qiniuOssProperties.getBucketName());

            try {
                uploadManager.put(byteInputStream, fileName, upToken, null, null);
            } catch (QiniuException ex) {
                Response r = ex.response;
                log.error("七牛云ERROR:{}", r.toString());
                try {
                    log.error("七牛云ERROR:{}", r.bodyString());
                } catch (QiniuException ex2) {
                    ex2.printStackTrace();
                }
                throw new RuntimeException("头像上传失败", ex);
            }
        } catch (Exception ex) {
            log.error("头像上传失败", ex);
            throw new RuntimeException("头像上传失败", ex);
        }

        String fileUrl = String.format("%s/%s", qiniuOssProperties.getDomainName(), fileName);
        log.info("Avatar uploaded successfully, size={}KB", compressedData.length / 1024);
        return fileUrl;
    }

    public String uploadBase64Image(String imageData, String folder) throws Exception {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("Image data must not be empty");
        }

        if (isOssUrl(imageData)) {
            log.info("Image is already on OSS, skipping upload: {}", imageData);
            return imageData;
        }

        // 处理 base64 图片数据
        String dataPrefix;
        String base64Data = imageData;
        if (base64Data.startsWith("data:image/")) {
            int commaIndex = base64Data.indexOf(",");
            dataPrefix = base64Data.substring(0, commaIndex);
            base64Data = base64Data.substring(commaIndex + 1);
        } else {
            dataPrefix = "data:image/jpeg;base64";
        }

        String extension;
        if (dataPrefix.contains("png")) {
            extension = ".png";
        } else if (dataPrefix.contains("gif")) {
            extension = ".gif";
        } else if (dataPrefix.contains("webp")) {
            extension = ".webp";
        } else {
            extension = ".jpg";
        }

        String fileName = folder + "/" + UUID.randomUUID().toString() + extension;

        // 3. 构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());  // 使用华东-浙江2区，与 yayfolk bucket 同区域
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        UploadManager uploadManager = new UploadManager(cfg);

        try {
            byte[] bytes = Base64.getDecoder().decode(base64Data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

            Auth auth = Auth.create(qiniuOssProperties.getAccessKey(), qiniuOssProperties.getSecretKey());
            String upToken = auth.uploadToken(qiniuOssProperties.getBucketName());

            try {
                uploadManager.put(inputStream, fileName, upToken, null, null);
            } catch (QiniuException ex) {
                Response r = ex.response;
                log.error("七牛云ERROR:{}", r.toString());
                try {
                    log.error("七牛云ERROR:{}", r.bodyString());
                } catch (QiniuException ex2) {
                    ex2.printStackTrace();
                }
                throw new RuntimeException("图片上传失败", ex);
            }

            return String.format("%s/%s", qiniuOssProperties.getDomainName(), fileName);
        } catch (Exception e) {
            log.warn("Base64 decode failed, returning original data: {}", e.getMessage());
            return imageData;
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String fileName = fileUrl.replace(qiniuOssProperties.getDomainName(), "");
            while (fileName.startsWith("/")) {
                fileName = fileName.substring(1);
            }

            Auth auth = Auth.create(qiniuOssProperties.getAccessKey(), qiniuOssProperties.getSecretKey());
            com.qiniu.storage.BucketManager bucketManager = new com.qiniu.storage.BucketManager(auth, new Configuration(Region.region2()));  // 使用华东-浙江2区
            bucketManager.delete(qiniuOssProperties.getBucketName(), fileName);
        } catch (Exception e) {
            log.error("File deletion failed: {}", fileUrl, e);
        }
    }

    public boolean isOssUrl(String url) {
        return url != null && url.startsWith(qiniuOssProperties.getDomainName());
    }

    public String uploadPostImage(MultipartFile file, Long postId, int index) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // 1. 获取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = "posts/" + postId + "/" + index + suffix;

        // 2. 构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());  // 使用华东-浙江2区，与 yayfolk bucket 同区域
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        UploadManager uploadManager = new UploadManager(cfg);

        // 3. 生成上传凭证，然后准备上传
        try {
            byte[] uploadBytes = file.getBytes();
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(uploadBytes);
            Auth auth = Auth.create(qiniuOssProperties.getAccessKey(), qiniuOssProperties.getSecretKey());
            String upToken = auth.uploadToken(qiniuOssProperties.getBucketName());

            try {
                uploadManager.put(byteInputStream, fileName, upToken, null, null);
            } catch (QiniuException ex) {
                Response r = ex.response;
                log.error("七牛云ERROR:{}", r.toString());
                try {
                    log.error("七牛云ERROR:{}", r.bodyString());
                } catch (QiniuException ex2) {
                    ex2.printStackTrace();
                }
                throw new RuntimeException("帖子图片上传失败", ex);
            }
        } catch (Exception ex) {
            log.error("帖子图片上传失败", ex);
            throw new RuntimeException("帖子图片上传失败", ex);
        }

        String fileUrl = String.format("%s/%s", qiniuOssProperties.getDomainName(), fileName);
        log.info("Post image uploaded successfully: postId={}, index={}, url={}", postId, index, fileUrl);
        return fileUrl;
    }

    public String uploadActivityImage(MultipartFile file, Long activityId, int index) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // 1. 获取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = "activities/" + activityId + "/images/" + index + suffix;

        // 2. 构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());  // 使用华东-浙江2区，与 yayfolk bucket 同区域
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        UploadManager uploadManager = new UploadManager(cfg);

        // 3. 生成上传凭证，然后准备上传
        try {
            byte[] uploadBytes = file.getBytes();
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(uploadBytes);
            Auth auth = Auth.create(qiniuOssProperties.getAccessKey(), qiniuOssProperties.getSecretKey());
            String upToken = auth.uploadToken(qiniuOssProperties.getBucketName());

            try {
                uploadManager.put(byteInputStream, fileName, upToken, null, null);
            } catch (QiniuException ex) {
                Response r = ex.response;
                log.error("七牛云ERROR:{}", r.toString());
                try {
                    log.error("七牛云ERROR:{}", r.bodyString());
                } catch (QiniuException ex2) {
                    ex2.printStackTrace();
                }
                throw new RuntimeException("活动图片上传失败", ex);
            }
        } catch (Exception ex) {
            log.error("活动图片上传失败", ex);
            throw new RuntimeException("活动图片上传失败", ex);
        }

        String fileUrl = String.format("%s/%s", qiniuOssProperties.getDomainName(), fileName);
        log.info("Activity image uploaded successfully: activityId={}, index={}, url={}", activityId, index, fileUrl);
        return fileUrl;
    }

    public String uploadActivityVideo(MultipartFile file, Long activityId, int index) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // 1. 获取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = "activities/" + activityId + "/video/" + index + suffix;

        // 2. 构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());  // 使用华东-浙江2区，与 yayfolk bucket 同区域
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        UploadManager uploadManager = new UploadManager(cfg);

        // 3. 生成上传凭证，然后准备上传
        try {
            byte[] uploadBytes = file.getBytes();
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(uploadBytes);
            Auth auth = Auth.create(qiniuOssProperties.getAccessKey(), qiniuOssProperties.getSecretKey());
            String upToken = auth.uploadToken(qiniuOssProperties.getBucketName());

            try {
                uploadManager.put(byteInputStream, fileName, upToken, null, null);
            } catch (QiniuException ex) {
                Response r = ex.response;
                log.error("七牛云ERROR:{}", r.toString());
                try {
                    log.error("七牛云ERROR:{}", r.bodyString());
                } catch (QiniuException ex2) {
                    ex2.printStackTrace();
                }
                throw new RuntimeException("活动视频上传失败", ex);
            }
        } catch (Exception ex) {
            log.error("活动视频上传失败", ex);
            throw new RuntimeException("活动视频上传失败", ex);
        }

        String fileUrl = String.format("%s/%s", qiniuOssProperties.getDomainName(), fileName);
        log.info("Activity video uploaded successfully: activityId={}, index={}, url={}", activityId, index, fileUrl);
        return fileUrl;
    }

    public String uploadHomepageImage(MultipartFile file, Long userId, String type) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // 1. 获取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        
        // 2. 构造文件路径，使用 userId 命名
        String fileName = "homepage/" + userId + (type != null ? "/" + type : "") + suffix;

        // 3. 构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());  // 使用华东-浙江2区，与 yayfolk bucket 同区域
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        UploadManager uploadManager = new UploadManager(cfg);

        // 4. 生成上传凭证，然后准备上传
        try {
            byte[] uploadBytes = file.getBytes();
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(uploadBytes);
            Auth auth = Auth.create(qiniuOssProperties.getAccessKey(), qiniuOssProperties.getSecretKey());
            String upToken = auth.uploadToken(qiniuOssProperties.getBucketName());

            try {
                uploadManager.put(byteInputStream, fileName, upToken, null, null);
            } catch (QiniuException ex) {
                Response r = ex.response;
                log.error("七牛云ERROR:{}", r.toString());
                try {
                    log.error("七牛云ERROR:{}", r.bodyString());
                } catch (QiniuException ex2) {
                    ex2.printStackTrace();
                }
                throw new RuntimeException("个人主页图片上传失败", ex);
            }
        } catch (Exception ex) {
            log.error("个人主页图片上传失败", ex);
            throw new RuntimeException("个人主页图片上传失败", ex);
        }

        String fileUrl = String.format("%s/%s", qiniuOssProperties.getDomainName(), fileName);
        log.info("Homepage image uploaded successfully: userId={}, type={}, url={}", userId, type, fileUrl);
        return fileUrl;
    }

    public String uploadActivityVR(MultipartFile file, Long activityId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        String fileName = "activities/" + activityId + "/VR/vr" + suffix;

        Configuration cfg = new Configuration(Region.region2());  // 使用华东-浙江2区，与 yayfolk bucket 同区域
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        UploadManager uploadManager = new UploadManager(cfg);

        try {
            byte[] uploadBytes = file.getBytes();
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(uploadBytes);
            Auth auth = Auth.create(qiniuOssProperties.getAccessKey(), qiniuOssProperties.getSecretKey());
            String upToken = auth.uploadToken(qiniuOssProperties.getBucketName());

            try {
                uploadManager.put(byteInputStream, fileName, upToken, null, null);
            } catch (QiniuException ex) {
                Response r = ex.response;
                log.error("七牛云ERROR:{}", r.toString());
                try {
                    log.error("七牛云ERROR:{}", r.bodyString());
                } catch (QiniuException ex2) {
                    ex2.printStackTrace();
                }
                throw new RuntimeException("VR模型上传失败", ex);
            }
        } catch (Exception ex) {
            log.error("VR模型上传失败", ex);
            throw new RuntimeException("VR模型上传失败", ex);
        }

        String fileUrl = String.format("%s/%s", qiniuOssProperties.getDomainName(), fileName);
        log.info("Activity VR model uploaded successfully: activityId={}, url={}", activityId, fileUrl);
        return fileUrl;
    }
}