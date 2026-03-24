package com.travelate.backend.controller;

import com.travelate.backend.dto.ResponseDto;
import com.travelate.backend.service.LandmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/landmark")
@CrossOrigin(origins = "*")
public class LandmarkController {

    @Autowired
    private LandmarkService landmarkService;

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseDto recognizeLandmark(@RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            return ResponseDto.error(400, "请上传图片");
        }
        return landmarkService.recognizeLandmark(image);
    }

    @PostMapping("/recognize-base64")
    public ResponseDto recognizeLandmarkBase64(@RequestBody Map<String, Object> request) {
        Object imgObj = request.get("image");
        String base64Image = imgObj != null ? imgObj.toString() : null;
        Double lat = null;
        Double lng = null;
        Integer radius = null;
        try {
            if (request.get("lat") != null) lat = Double.valueOf(request.get("lat").toString());
            if (request.get("lng") != null) lng = Double.valueOf(request.get("lng").toString());
            if (request.get("radius") != null) radius = Integer.valueOf(request.get("radius").toString());
        } catch (Exception ignored) {}
        if (base64Image == null || base64Image.isEmpty()) {
            return ResponseDto.error(400, "请提供图片数据");
        }
        return landmarkService.recognizeLandmarkBase64(base64Image, lat, lng, radius);
    }
}
