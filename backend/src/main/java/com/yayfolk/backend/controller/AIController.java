package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.AIHeritageRouteService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIHeritageRouteService aiHeritageRouteService;

    public AIController(AIHeritageRouteService aiHeritageRouteService) {
        this.aiHeritageRouteService = aiHeritageRouteService;
    }

    @PostMapping("/heritage-route")
    public ResponseDto generateHeritageRoute(@RequestBody Map<String, Object> request) {
        try {
            return ResponseDto.success(aiHeritageRouteService.generateRoute(request));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
}
