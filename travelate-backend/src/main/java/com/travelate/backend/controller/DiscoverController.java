package com.travelate.backend.controller;

import com.travelate.backend.dto.ResponseDto;
import com.travelate.backend.service.DiscoverService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {
    private final DiscoverService discoverService;

    public DiscoverController(DiscoverService discoverService) {
        this.discoverService = discoverService;
    }

    @GetMapping("/posts")
    public ResponseDto getPosts(@RequestParam(required = false) String category,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false, defaultValue = "latest") String sortBy,
                                HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.getFeed(username, category, keyword, sortBy));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/posts/{id}")
    public ResponseDto getPostDetail(@PathVariable("id") Long postId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.getPostDetail(username, postId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/posts")
    public ResponseDto createPost(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.createPost(username, payload));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/posts/{id}")
    public ResponseDto updatePost(@PathVariable("id") Long postId,
                                  @RequestBody Map<String, Object> payload,
                                  HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.updatePost(username, postId, payload));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/posts/{id}/collect")
    public ResponseDto toggleCollect(@PathVariable("id") Long postId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.toggleCollection(username, postId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/posts/{id}/comments")
    public ResponseDto addComment(@PathVariable("id") Long postId,
                                  @RequestBody Map<String, String> payload,
                                  HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.addComment(username, postId, payload));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseDto toggleCommentLike(@PathVariable("commentId") Long commentId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.toggleCommentLike(username, commentId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseDto deleteComment(@PathVariable("commentId") Long commentId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            discoverService.deleteComment(username, commentId);
            return ResponseDto.success("删除成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/my/posts")
    public ResponseDto myPosts(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.getMyPosts(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/my/posts/{id}")
    public ResponseDto deleteMyPost(@PathVariable("id") Long postId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            discoverService.deleteMyPost(username, postId);
            return ResponseDto.success("删除成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/my/collections")
    public ResponseDto myCollections(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.getMyCollections(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/my/collections/{postId}")
    public ResponseDto removeCollection(@PathVariable Long postId, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            discoverService.removeMyCollection(username, postId);
            return ResponseDto.success("取消收藏成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/my/history")
    public ResponseDto myHistory(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.getMyHistory(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/my/history")
    public ResponseDto clearHistory(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            discoverService.clearMyHistory(username);
            return ResponseDto.success("清空成功");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/my/stats")
    public ResponseDto myStats(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(discoverService.getMyStats(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/share/{id}")
    public ResponseDto getSharePostDetail(@PathVariable("id") Long postId) {
        try {
            return ResponseDto.success(discoverService.getSharePostDetail(postId));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    private String requireUsername(HttpServletRequest request) {
        Object usernameObj = request.getAttribute("username");
        if (usernameObj == null) {
            throw new RuntimeException("未授权，请先登录");
        }
        return usernameObj.toString();
    }
}
