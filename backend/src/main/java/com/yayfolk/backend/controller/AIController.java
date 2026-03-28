package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.AIHeritageRouteService;
import com.yayfolk.backend.service.RoutePlanService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIHeritageRouteService aiHeritageRouteService;
    private final RoutePlanService routePlanService;

    public AIController(AIHeritageRouteService aiHeritageRouteService,
                        RoutePlanService routePlanService) {
        this.aiHeritageRouteService = aiHeritageRouteService;
        this.routePlanService = routePlanService;
    }

    @PostMapping("/heritage-route")
    public ResponseDto generateHeritageRoute(@RequestBody Map<String, Object> request) {
        try {
            return ResponseDto.success(aiHeritageRouteService.generateRoute(request));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/heritage-route/favorites")
    public ResponseDto saveFavoriteRoute(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            return ResponseDto.success(routePlanService.saveFavorite(username, request));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/heritage-route/favorites")
    public ResponseDto getFavoriteRoutes(HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            List<Map<String, Object>> favorites = routePlanService.getFavorites(username);
            return ResponseDto.success(favorites);
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/heritage-route/favorites/{id}")
    public ResponseDto getFavoriteRouteDetail(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            return ResponseDto.success(routePlanService.getFavoriteDetail(username, id));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/heritage-route/favorites/{id}")
    public ResponseDto deleteFavoriteRoute(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String username = currentUsername(httpRequest);
            routePlanService.deleteFavorite(username, id);
            return ResponseDto.success("删除成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    private String currentUsername(HttpServletRequest httpRequest) {
        Object usernameObj = httpRequest.getAttribute("username");
        if (usernameObj == null) {
            throw new RuntimeException("未授权，请先登录");
        }
        return usernameObj.toString();
    }
}
