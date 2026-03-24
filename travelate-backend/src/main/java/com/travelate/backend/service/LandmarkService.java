package com.travelate.backend.service;

import com.travelate.backend.dto.ResponseDto;
import com.travelate.backend.entity.Attraction;
import com.travelate.backend.entity.AttractionImage;
import com.travelate.backend.repository.AttractionRepository;
import com.travelate.backend.repository.AttractionImageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class LandmarkService {

    @Value("${landmark.model.url:http://localhost:5000}")
    private String modelServiceUrl;

    @Autowired
    private AttractionRepository attractionRepository;

    @Autowired
    private AttractionImageRepository attractionImageRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 使用 Haversine 公式计算两点之间的距离（单位：公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public ResponseDto recognizeLandmark(MultipartFile image) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", image.getResource());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    modelServiceUrl + "/predict",
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String predictedLabel = root.get("predicted").asText();
                double confidence = root.get("confidence").asDouble();

                Optional<Attraction> attractionOpt = attractionRepository.findByName(predictedLabel);
                
                Map<String, Object> result = new HashMap<>();
                result.put("name", predictedLabel);
                result.put("confidence", Math.round(confidence * 100.0) / 100.0);

                if (attractionOpt.isPresent()) {
                    Attraction attraction = attractionOpt.get();
                    result.put("intro", attraction.getIntro());
                    result.put("story", attraction.getStory());
                    result.put("cover", attraction.getCover());
                    result.put("rating", attraction.getRating());
                    result.put("duration", attraction.getDuration());
                    result.put("bestSeason", attraction.getBestSeason());
                    
                    // 查询图片集
                    List<String> images = new ArrayList<>();
                    images.add(attraction.getCover());
                    
                    // 查询景点的其他图片
                    List<AttractionImage> attractionImages = attractionImageRepository.findByAttractionIdOrderByOrderIndexAsc(attraction.getId());
                    for (AttractionImage attractionImage : attractionImages) {
                        images.add(attractionImage.getImageUrl());
                    }
                    
                    result.put("images", images);
                    
                    List<Attraction> nearbyAttractions = attractionRepository.findNearbyAttractions(
                            attraction.getLng().doubleValue(),
                            attraction.getLat().doubleValue(),
                            10000,
                            5
                    );
                    
                    List<Map<String, Object>> nearbyList = new ArrayList<>();
                    for (Attraction nearby : nearbyAttractions) {
                        if (!nearby.getId().equals(attraction.getId())) {
                            // 计算距离
                            double distance = calculateDistance(
                                    attraction.getLat().doubleValue(),
                                    attraction.getLng().doubleValue(),
                                    nearby.getLat().doubleValue(),
                                    nearby.getLng().doubleValue()
                            );
                            String distanceStr;
                            if (distance < 1) {
                                distanceStr = String.format("%.0f米", distance * 1000);
                            } else {
                                distanceStr = String.format("%.1f公里", distance);
                            }
                            
                            Map<String, Object> nearbyItem = new HashMap<>();
                            nearbyItem.put("id", nearby.getId());
                            nearbyItem.put("name", nearby.getName());
                            nearbyItem.put("cover", nearby.getCover());
                            nearbyItem.put("rating", nearby.getRating());
                            nearbyItem.put("duration", nearby.getDuration());
                            nearbyItem.put("bestSeason", nearby.getBestSeason());
                            nearbyItem.put("intro", nearby.getIntro());
                            nearbyItem.put("story", nearby.getStory());
                            nearbyItem.put("distance", distanceStr);
                            nearbyList.add(nearbyItem);
                        }
                    }
                    result.put("nearbyAttractions", nearbyList);
                }

                return ResponseDto.success(result);
            } else {
                return ResponseDto.error(500, "模型服务调用失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.error(500, "识别失败: " + e.getMessage());
        }
    }

    public ResponseDto recognizeLandmarkBase64(String base64Image) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("image", base64Image);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    modelServiceUrl + "/predict",
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String predictedLabel = root.get("predicted").asText();
                double confidence = root.get("confidence").asDouble();

                Optional<Attraction> attractionOpt = attractionRepository.findByName(predictedLabel);

                Map<String, Object> result = new HashMap<>();
                result.put("name", predictedLabel);
                result.put("confidence", Math.round(confidence * 100.0) / 100.0);

                if (attractionOpt.isPresent()) {
                    Attraction attraction = attractionOpt.get();
                    result.put("intro", attraction.getIntro());
                    result.put("story", attraction.getStory());
                    result.put("cover", attraction.getCover());
                    result.put("rating", attraction.getRating());
                    result.put("duration", attraction.getDuration());
                    result.put("bestSeason", attraction.getBestSeason());
                    
                    // 查询图片集
                    List<String> images = new ArrayList<>();
                    images.add(attraction.getCover());
                    
                    // 查询景点的其他图片
                    List<AttractionImage> attractionImages = attractionImageRepository.findByAttractionIdOrderByOrderIndexAsc(attraction.getId());
                    for (AttractionImage attractionImage : attractionImages) {
                        images.add(attractionImage.getImageUrl());
                    }
                    
                    result.put("images", images);

                    List<Attraction> nearbyAttractions = attractionRepository.findNearbyAttractions(
                            attraction.getLng().doubleValue(),
                            attraction.getLat().doubleValue(),
                            10000,
                            5
                    );

                    List<Map<String, Object>> nearbyList = new ArrayList<>();
                    for (Attraction nearby : nearbyAttractions) {
                        if (!nearby.getId().equals(attraction.getId())) {
                            // 计算距离
                            double distance = calculateDistance(
                                    attraction.getLat().doubleValue(),
                                    attraction.getLng().doubleValue(),
                                    nearby.getLat().doubleValue(),
                                    nearby.getLng().doubleValue()
                            );
                            String distanceStr;
                            if (distance < 1) {
                                distanceStr = String.format("%.0f米", distance * 1000);
                            } else {
                                distanceStr = String.format("%.1f公里", distance);
                            }

                            Map<String, Object> nearbyItem = new HashMap<>();
                            nearbyItem.put("id", nearby.getId());
                            nearbyItem.put("name", nearby.getName());
                            nearbyItem.put("cover", nearby.getCover());
                            nearbyItem.put("rating", nearby.getRating());
                            nearbyItem.put("duration", nearby.getDuration());
                            nearbyItem.put("bestSeason", nearby.getBestSeason());
                            nearbyItem.put("intro", nearby.getIntro());
                            nearbyItem.put("story", nearby.getStory());
                            nearbyItem.put("distance", distanceStr);
                            nearbyList.add(nearbyItem);
                        }
                    }
                    result.put("nearbyAttractions", nearbyList);
                }

                return ResponseDto.success(result);
            } else {
                return ResponseDto.error(500, "模型服务调用失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.error(500, "识别失败: " + e.getMessage());
        }
    }

    public ResponseDto recognizeLandmarkBase64(String base64Image, Double lat, Double lng, Integer radius) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("image", base64Image);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    modelServiceUrl + "/predict",
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String predictedLabel = root.hasNonNull("predicted") ? root.get("predicted").asText() : null;
                double confidence = root.hasNonNull("confidence") ? root.get("confidence").asDouble() : 0.0;
                List<Map<String, Object>> candidates = new ArrayList<>();
                if (root.has("candidates") && root.get("candidates").isArray()) {
                    for (JsonNode n : root.get("candidates")) {
                        if (n.hasNonNull("label") && n.hasNonNull("confidence")) {
                            Map<String, Object> c = new HashMap<>();
                            c.put("label", n.get("label").asText());
                            c.put("confidence", n.get("confidence").asDouble());
                            candidates.add(c);
                        }
                    }
                    if (!candidates.isEmpty() && (predictedLabel == null || predictedLabel.isEmpty())) {
                        candidates.sort((a,b) -> Double.compare((double)b.get("confidence"), (double)a.get("confidence")));
                        predictedLabel = (String) candidates.get(0).get("label");
                        confidence = (double) candidates.get(0).get("confidence");
                    }
                } else if (root.has("all_predictions") && root.get("all_predictions").isArray()) {
                    for (JsonNode n : root.get("all_predictions")) {
                        if (n.hasNonNull("label") && n.hasNonNull("confidence")) {
                            Map<String, Object> c = new HashMap<>();
                            c.put("label", n.get("label").asText());
                            c.put("confidence", n.get("confidence").asDouble() / 100.0);
                            candidates.add(c);
                        }
                    }
                    if (!candidates.isEmpty() && (predictedLabel == null || predictedLabel.isEmpty())) {
                        candidates.sort((a,b) -> Double.compare((double)b.get("confidence"), (double)a.get("confidence")));
                        predictedLabel = (String) candidates.get(0).get("label");
                        confidence = (double) candidates.get(0).get("confidence");
                    }
                }

                Optional<Attraction> attractionOpt;
                List<String> debug = new ArrayList<>();
                String strategy = "unknown";
                if (lat != null && lng != null) {
                    int r = (radius != null && radius > 0) ? radius : 30000;
                    attractionOpt = (predictedLabel != null && !predictedLabel.isEmpty())
                            ? attractionRepository.findByNameWithinRadius(lng, lat, r, predictedLabel)
                            : Optional.empty();
                    if (!attractionOpt.isPresent()) {
                        debug.add("范围内按名称未匹配：" + (predictedLabel != null ? predictedLabel : "null"));
                    }
                    if (!attractionOpt.isPresent() && !candidates.isEmpty()) {
                        List<Attraction> nearbyPool = attractionRepository.findNearbyAttractions(lng, lat, r, 50);
                        if (!nearbyPool.isEmpty()) {
                            Attraction best = null;
                            double bestScore = -1.0;
                            String bestLabel = predictedLabel;
                            double bestConf = confidence;
                            for (Attraction candAttr : nearbyPool) {
                                String attrNameNorm = normalizeName(candAttr.getName());
                                for (Map<String, Object> c : candidates) {
                                    String lbl = normalizeName((String) c.get("label"));
                                    double conf = (double) c.get("confidence");
                                    double sim = similarity(lbl, attrNameNorm);
                                    double score = 0.6 * conf + 0.4 * sim;
                                    if (score > bestScore) {
                                        bestScore = score;
                                        best = candAttr;
                                        bestLabel = (String) c.get("label");
                                        bestConf = conf;
                                    }
                                }
                            }
                            if (best != null) {
                                attractionOpt = Optional.of(best);
                                predictedLabel = bestLabel;
                                confidence = bestConf;
                                strategy = "range_similarity_match";
                                debug.add("范围内按候选相似度匹配：" + best.getName());
                            }
                        } else {
                            debug.add("范围内无景点");
                        }
                    } else if (attractionOpt.isPresent()) {
                        strategy = "range_name_match";
                        debug.add("范围内按名称匹配成功：" + attractionOpt.get().getName());
                    } else if (candidates.isEmpty()) {
                        debug.add("模型未返回候选，无法进行相似度匹配");
                    }
                    if (!attractionOpt.isPresent() && predictedLabel != null) {
                        attractionOpt = attractionRepository.findByName(predictedLabel);
                        if (attractionOpt.isPresent()) {
                            strategy = "global_name_match";
                            debug.add("全局按名称匹配成功：" + attractionOpt.get().getName());
                        } else {
                            strategy = "no_match";
                            debug.add("全局按名称未匹配");
                        }
                    }
                } else {
                    attractionOpt = attractionRepository.findByName(predictedLabel);
                    if (attractionOpt.isPresent()) {
                        strategy = "global_name_match";
                        debug.add("未提供坐标，全局按名称匹配成功：" + attractionOpt.get().getName());
                    } else {
                        strategy = "no_match";
                        debug.add("未提供坐标，全局按名称未匹配");
                    }
                }

                Map<String, Object> result = new HashMap<>();
                result.put("name", predictedLabel);
                result.put("confidence", Math.round(confidence * 100.0) / 100.0);
                if (!candidates.isEmpty()) {
                    result.put("candidates", candidates);
                }
                result.put("strategy", strategy);
                result.put("debug", debug);

                if (attractionOpt.isPresent()) {
                    Attraction attraction = attractionOpt.get();
                    result.put("intro", attraction.getIntro());
                    result.put("story", attraction.getStory());
                    result.put("cover", attraction.getCover());
                    result.put("rating", attraction.getRating());
                    result.put("duration", attraction.getDuration());
                    result.put("bestSeason", attraction.getBestSeason());

                    List<String> images = new ArrayList<>();
                    images.add(attraction.getCover());
                    List<AttractionImage> attractionImages = attractionImageRepository.findByAttractionIdOrderByOrderIndexAsc(attraction.getId());
                    for (AttractionImage attractionImage : attractionImages) {
                        images.add(attractionImage.getImageUrl());
                    }
                    result.put("images", images);

                    List<Attraction> nearbyAttractions = attractionRepository.findNearbyAttractions(
                            attraction.getLng().doubleValue(),
                            attraction.getLat().doubleValue(),
                            10000,
                            5
                    );

                    List<Map<String, Object>> nearbyList = new ArrayList<>();
                    for (Attraction nearby : nearbyAttractions) {
                        if (!nearby.getId().equals(attraction.getId())) {
                            double distance = calculateDistance(
                                    attraction.getLat().doubleValue(),
                                    attraction.getLng().doubleValue(),
                                    nearby.getLat().doubleValue(),
                                    nearby.getLng().doubleValue()
                            );
                            String distanceStr;
                            if (distance < 1) {
                                distanceStr = String.format("%.0f米", distance * 1000);
                            } else {
                                distanceStr = String.format("%.1f公里", distance);
                            }
                            Map<String, Object> nearbyItem = new HashMap<>();
                            nearbyItem.put("id", nearby.getId());
                            nearbyItem.put("name", nearby.getName());
                            nearbyItem.put("cover", nearby.getCover());
                            nearbyItem.put("rating", nearby.getRating());
                            nearbyItem.put("duration", nearby.getDuration());
                            nearbyItem.put("bestSeason", nearby.getBestSeason());
                            nearbyItem.put("intro", nearby.getIntro());
                            nearbyItem.put("story", nearby.getStory());
                            nearbyItem.put("distance", distanceStr);
                            nearbyList.add(nearbyItem);
                        }
                    }
                    result.put("nearbyAttractions", nearbyList);
                }

                return ResponseDto.success(result);
            } else {
                return ResponseDto.error(500, "模型服务调用失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.error(500, "识别失败: " + e.getMessage());
        }
    }

    private String normalizeName(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[\\s\\p{Punct}]+", "");
    }

    private int levenshtein(String a, String b) {
        int n = a.length();
        int m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                int del = prev[j] + 1;
                int ins = curr[j - 1] + 1;
                int sub = prev[j - 1] + cost;
                int v = Math.min(del, ins);
                curr[j] = Math.min(v, sub);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[m];
    }

    private double similarity(String a, String b) {
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        int d = levenshtein(a, b);
        return 1.0 - (double) d / maxLen;
    }
}
