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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

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
            log.info("使用HTTP代理: {}:{}", httpProxyHost, httpProxyPort);
        }
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("OSS客户端初始化成功");
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS客户端已关闭");
        }
    }

    private OSS getOssClient() {
        return ossClient;
    }

    public String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        OSS ossClient = getOssClient();
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = folder + "/" + UUID.randomUUID().toString() + extension;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata);
            ossClient.putObject(putObjectRequest);

            return baseUrl + fileName;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    private byte[] compressImage(MultipartFile file, int maxSizeKB) throws IOException {
        // 先将输入流数据缓存到内存中，以便多次读取
        byte[] originalData = file.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalData);
        BufferedImage image = ImageIO.read(inputStream);
        
        if (image == null) {
            // 如果不是图片文件，直接返回原始数据
            log.warn("不是有效的图片文件，使用原始数据");
            return originalData;
        }
        
        // 计算压缩比例
        int targetSize = maxSizeKB * 1024;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // 获取原始文件的格式
        String originalFilename = file.getOriginalFilename();
        String format = "jpg";
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (extension.equals("png") || extension.equals("gif") || extension.equals("webp")) {
                format = extension;
            }
        }
        
        try {
            // 质量从高到低尝试压缩
            float quality = 0.9f;
            while (quality > 0.1f) {
                outputStream.reset();
                
                // 使用ImageWriter设置压缩质量
                javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName(format).next();
                javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
                
                // 检查是否支持压缩
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
            
            // 如果还是太大，调整尺寸
            if (outputStream.size() > targetSize) {
                outputStream.reset();
                int width = image.getWidth();
                int height = image.getHeight();
                int maxDimension = 300; // 最大尺寸
                
                if (width > maxDimension || height > maxDimension) {
                    double scale = Math.min((double) maxDimension / width, (double) maxDimension / height);
                    int newWidth = (int) (width * scale);
                    int newHeight = (int) (height * scale);
                    
                    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = resizedImage.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
                    g2d.dispose();
                    
                    // 再次压缩
                    javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName(format).next();
                    javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
                    
                    // 检查是否支持压缩
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
            // 如果压缩失败，直接返回原始图片数据
            log.warn("图片压缩失败，使用原始图片", e);
            outputStream.reset();
            outputStream.write(originalData);
        } finally {
            inputStream.close();
        }
        
        return outputStream.toByteArray();
    }

    public String uploadAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        OSS ossClient = getOssClient();
        try {
            // 压缩图片到20KB以下
            byte[] compressedData = compressImage(file, 20);
            
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = "avatars/" + userId + extension;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(compressedData.length);
            metadata.setContentType("image/jpeg");

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, 
                new ByteArrayInputStream(compressedData), metadata);
            ossClient.putObject(putObjectRequest);

            log.info("头像上传成功，压缩后大小：{}KB", compressedData.length / 1024);
            return baseUrl + fileName;
        } catch (IOException e) {
            log.error("头像上传失败", e);
            throw new RuntimeException("头像上传失败", e);
        }
    }

    public String uploadBase64Image(String imageData, String folder) {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("图片数据不能为空");
        }

        // 检查是否是OSS URL
        if (isOssUrl(imageData)) {
            log.info("图片已是OSS URL，跳过上传: {}", imageData);
            return imageData;
        }

        // 检查是否是本地文件路径（景点图片）
        if (imageData.startsWith("images/")) {
            OSS ossClient = getOssClient();
            try {
                // 构建本地文件路径 - 景点图片在cover/images目录下
                String filePath = "d:/Cursor/program/travelate/cover/" + imageData;
                File file = new File(filePath);
                if (file.exists()) {
                    String extension = imageData.substring(imageData.lastIndexOf("."));
                    String fileName = folder + "/" + UUID.randomUUID().toString() + extension;

                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(file.length());
                    metadata.setContentType("image/" + extension.substring(1));

                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, new FileInputStream(file), metadata);
                    ossClient.putObject(putObjectRequest);

                    String url = baseUrl + fileName;
                    log.info("本地文件上传成功: {}", url);
                    return url;
                } else {
                    log.warn("本地文件不存在: {}", filePath);
                    return imageData;
                }
            } catch (Exception e) {
                log.error("本地文件上传失败", e);
                return imageData;
            }
        }

        // 检查是否是景点封面（没有images/前缀的文件名）
        if (!imageData.startsWith("http") && !imageData.startsWith("data:image") && !imageData.startsWith("/")) {
            OSS ossClient = getOssClient();
            try {
                // 构建本地文件路径 - 景点封面在cover目录下
                String filePath = "d:/Cursor/program/travelate/cover/" + imageData;
                File file = new File(filePath);
                if (file.exists()) {
                    String extension = imageData.substring(imageData.lastIndexOf("."));
                    String fileName = folder + "/" + UUID.randomUUID().toString() + extension;

                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(file.length());
                    metadata.setContentType("image/" + extension.substring(1));

                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, new FileInputStream(file), metadata);
                    ossClient.putObject(putObjectRequest);

                    String url = baseUrl + fileName;
                    log.info("景点封面上传成功: {}", url);
                    return url;
                } else {
                    log.warn("景点封面文件不存在: {}", filePath);
                    return imageData;
                }
            } catch (Exception e) {
                log.error("景点封面上传失败", e);
                return imageData;
            }
        }

        // 处理Base64编码
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

            String fileName = folder + "/" + UUID.randomUUID().toString() + extension;

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
                log.warn("Base64解码失败，可能不是Base64数据: {}", e.getMessage());
                return imageData;
            }
        } catch (Exception e) {
            log.error("图片上传失败", e);
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
            log.error("文件删除失败：{}", fileUrl, e);
        }
    }

    public boolean isOssUrl(String url) {
        return url != null && url.startsWith(baseUrl);
    }

    /**
     * 上传帖子图片到指定postId文件夹，按序号命名
     * @param file 图片文件
     * @param postId 帖子ID
     * @param index 图片序号（从1开始）
     * @return 上传后的URL
     */
    public String uploadPostImage(MultipartFile file, Long postId, int index) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        OSS ossClient = getOssClient();
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 使用序号命名，如 posts/123/1.png
            String fileName = "posts/" + postId + "/" + index + extension;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata);
            ossClient.putObject(putObjectRequest);

            log.info("帖子图片上传成功: postId={}, index={}, url={}", postId, index, baseUrl + fileName);
            return baseUrl + fileName;
        } catch (IOException e) {
            log.error("帖子图片上传失败", e);
            throw new RuntimeException("帖子图片上传失败", e);
        }
    }
}
