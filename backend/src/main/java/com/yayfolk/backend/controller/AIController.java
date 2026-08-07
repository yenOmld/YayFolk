package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.MerchantAIService;
import com.yayfolk.backend.service.DoubaoHeritagePosterService;
import com.yayfolk.backend.service.Hunyuan3DService;
import com.yayfolk.backend.ai.vector.VectorIndexBuilder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

//  AI辅助生成：豆包API（Doubao-Seedream-5.0-lite），2025-04-24
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final DoubaoHeritagePosterService doubaoHeritagePosterService;
    private final Hunyuan3DService hunyuan3DService;
    private final MerchantAIService merchantAIService;
    private final VectorIndexBuilder vectorIndexBuilder;

    public AIController(DoubaoHeritagePosterService doubaoHeritagePosterService,
                        Hunyuan3DService hunyuan3DService,
                        MerchantAIService merchantAIService,
                        VectorIndexBuilder vectorIndexBuilder) {
        this.doubaoHeritagePosterService = doubaoHeritagePosterService;
        this.hunyuan3DService = hunyuan3DService;
        this.merchantAIService = merchantAIService;
        this.vectorIndexBuilder = vectorIndexBuilder;
    }

    @PostMapping("/rebuild-index")
    public ResponseDto rebuildIndex() {
        try {
            vectorIndexBuilder.buildIndex();
            return ResponseDto.success("向量索引重建成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/heritage-poster")
    public ResponseDto generateAiHeritagePoster(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = doubaoHeritagePosterService.generatePoster(request);
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    /**
     * 代理下载远程图片，解决前端跨域问题
     */
    @GetMapping("/proxy-image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam("url") String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // 只允许下载豆包API生成的图片
            if (!imageUrl.contains("volces.com") && !imageUrl.contains("doubao")) {
                return ResponseEntity.badRequest().build();
            }
            
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return ResponseEntity.status(responseCode).build();
            }
            
            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = "image/jpeg";
            }
            
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                byte[] imageBytes = outputStream.toByteArray();
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentLength(imageBytes.length);
                headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
                
                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/image-to-3d")
    public ResponseDto generateImageTo3D(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = hunyuan3DService.generate3D(request);
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    /**
     * 代理下载3D模型文件，解决前端跨域问题
     */
    @GetMapping("/proxy-3d")
    public ResponseEntity<byte[]> proxy3DModel(@RequestParam("url") String modelUrl) {
        try {
            if (modelUrl == null || modelUrl.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            String lowerUrl = modelUrl.toLowerCase();
            if (!lowerUrl.contains("ai3d") && !lowerUrl.contains("hunyuan") && !lowerUrl.contains("tencent")) {
                return ResponseEntity.badRequest().build();
            }

            URL url = new URL(modelUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return ResponseEntity.status(responseCode).build();
            }

            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = "model/gltf-binary";
            }

            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                byte[] modelBytes = outputStream.toByteArray();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentLength(modelBytes.length);
                headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));

                return new ResponseEntity<>(modelBytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/merchant-suggestions")
    public ResponseDto generateMerchantSuggestions(@RequestBody Map<String, Object> merchantData) {
        try {
            Map<String, Object> result = merchantAIService.generateMerchantSuggestions(merchantData);
            return ResponseDto.success(result);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
}
