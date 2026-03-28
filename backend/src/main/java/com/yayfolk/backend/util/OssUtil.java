package com.yayfolk.backend.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
public class OssUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.base-url}")
    private String baseUrl;

    @Value("${app.oss-root-folder:yayfolk}")
    private String ossRootFolder;

    @Value("${app.local-image-base-path:D:/Cursor/program/travelate/cover/}")
    private String localImageBasePath;

    @Value("${HTTP_PROXY_HOST:}")
    private String httpProxyHost;

    @Value("${HTTP_PROXY_PORT:}")
    private Integer httpProxyPort;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        if (httpProxyHost != null && !httpProxyHost.isEmpty() && httpProxyPort != null) {
            System.setProperty("http.proxyHost", httpProxyHost);
            System.setProperty("http.proxyPort", httpProxyPort.toString());
            System.setProperty("https.proxyHost", httpProxyHost);
            System.setProperty("https.proxyPort", httpProxyPort.toString());
            log.info("Using HTTP proxy: {}:{}", httpProxyHost, httpProxyPort);
        }
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("OSS client initialized");
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS client closed");
        }
    }

    private OSS getOssClient() {
        return ossClient;
    }

    private File resolveLocalImageFile(String relativePath) {
        String normalizedPath = relativePath == null ? "" : relativePath.replace("\\", "/");
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return new File(localImageBasePath, normalizedPath);
    }

    private String buildObjectKey(String relativePath) {
        String normalizedRoot = ossRootFolder == null ? "" : ossRootFolder.trim().replace("\\", "/");
        String normalizedRelativePath = relativePath == null ? "" : relativePath.trim().replace("\\", "/");

        while (normalizedRoot.startsWith("/")) {
            normalizedRoot = normalizedRoot.substring(1);
        }
        while (normalizedRoot.endsWith("/")) {
            normalizedRoot = normalizedRoot.substring(0, normalizedRoot.length() - 1);
        }
        while (normalizedRelativePath.startsWith("/")) {
            normalizedRelativePath = normalizedRelativePath.substring(1);
        }

        if (normalizedRoot.isEmpty()) {
            return normalizedRelativePath;
        }
        if (normalizedRelativePath.isEmpty()) {
            return normalizedRoot;
        }
        return normalizedRoot + "/" + normalizedRelativePath;
    }

    public String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        OSS ossClient = getOssClient();
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = buildObjectKey(folder + "/" + UUID.randomUUID().toString() + extension);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata);
            ossClient.putObject(putObjectRequest);

            return baseUrl + fileName;
        } catch (IOException e) {
            log.error("File upload failed", e);
            throw new RuntimeException("File upload failed", e);
        }
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

    public String uploadAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        OSS ossClient = getOssClient();
        try {
            byte[] compressedData = compressImage(file, 20);

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = buildObjectKey("avatars/" + userId + extension);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(compressedData.length);
            metadata.setContentType("image/jpeg");

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName,
                new ByteArrayInputStream(compressedData), metadata);
            ossClient.putObject(putObjectRequest);

            log.info("Avatar uploaded successfully, size={}KB", compressedData.length / 1024);
            return baseUrl + fileName;
        } catch (IOException e) {
            log.error("Avatar upload failed", e);
            throw new RuntimeException("Avatar upload failed", e);
        }
    }

    public String uploadBase64Image(String imageData, String folder) {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("Image data must not be empty");
        }

        if (isOssUrl(imageData)) {
            log.info("Image is already on OSS, skipping upload: {}", imageData);
            return imageData;
        }

        if (imageData.startsWith("images/")) {
            OSS ossClient = getOssClient();
            try {
                File file = resolveLocalImageFile(imageData);
                if (file.exists()) {
                    String extension = imageData.substring(imageData.lastIndexOf("."));
                    String fileName = buildObjectKey(folder + "/" + UUID.randomUUID().toString() + extension);

                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(file.length());
                    metadata.setContentType("image/" + extension.substring(1));

                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, new FileInputStream(file), metadata);
                    ossClient.putObject(putObjectRequest);

                    String url = baseUrl + fileName;
                    log.info("Local image uploaded successfully: {}", url);
                    return url;
                } else {
                    log.warn("Local image file does not exist: {}", file.getAbsolutePath());
                    return imageData;
                }
            } catch (Exception e) {
                log.error("Local image upload failed", e);
                return imageData;
            }
        }

        if (!imageData.startsWith("http") && !imageData.startsWith("data:image") && !imageData.startsWith("/")) {
            OSS ossClient = getOssClient();
            try {
                File file = resolveLocalImageFile(imageData);
                if (file.exists()) {
                    String extension = imageData.substring(imageData.lastIndexOf("."));
                    String fileName = buildObjectKey(folder + "/" + UUID.randomUUID().toString() + extension);

                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(file.length());
                    metadata.setContentType("image/" + extension.substring(1));

                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, new FileInputStream(file), metadata);
                    ossClient.putObject(putObjectRequest);

                    String url = baseUrl + fileName;
                    log.info("Local cover uploaded successfully: {}", url);
                    return url;
                } else {
                    log.warn("Local cover file does not exist: {}", file.getAbsolutePath());
                    return imageData;
                }
            } catch (Exception e) {
                log.error("Local cover upload failed", e);
                return imageData;
            }
        }

        OSS ossClient = getOssClient();
        try {
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

            String fileName = buildObjectKey(folder + "/" + UUID.randomUUID().toString() + extension);

            try {
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                InputStream inputStream = new ByteArrayInputStream(bytes);

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(bytes.length);
                metadata.setContentType("image/" + extension.substring(1));

                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, metadata);
                ossClient.putObject(putObjectRequest);

                return baseUrl + fileName;
            } catch (Exception e) {
                log.warn("Base64 decode failed, returning original data: {}", e.getMessage());
                return imageData;
            }
        } catch (Exception e) {
            log.error("Image upload failed", e);
            return imageData;
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        OSS ossClient = getOssClient();
        try {
            String fileName = fileUrl.replace(baseUrl, "");
            ossClient.deleteObject(bucketName, fileName);
        } catch (Exception e) {
            log.error("File deletion failed: {}", fileUrl, e);
        }
    }

    public boolean isOssUrl(String url) {
        return url != null && url.startsWith(baseUrl);
    }

    public String uploadPostImage(MultipartFile file, Long postId, int index) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        OSS ossClient = getOssClient();
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = buildObjectKey("posts/" + postId + "/" + index + extension);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata);
            ossClient.putObject(putObjectRequest);

            log.info("Post image uploaded successfully: postId={}, index={}, url={}", postId, index, baseUrl + fileName);
            return baseUrl + fileName;
        } catch (IOException e) {
            log.error("Post image upload failed", e);
            throw new RuntimeException("Post image upload failed", e);
        }
    }
}
