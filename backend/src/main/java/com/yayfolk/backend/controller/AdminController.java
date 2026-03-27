package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.AdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private String requireUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        if (username == null) {
            throw new RuntimeException("Unauthorized, please login first");
        }
        return username.toString();
    }

    @GetMapping("/merchants")
    public ResponseDto getMerchants(@RequestParam(required = false) String status,
                                    HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getMerchantApplications(username, status));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/merchants/{id}/audit")
    public ResponseDto auditMerchant(@PathVariable Long id,
                                     @RequestBody Map<String, Object> data,
                                     HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            boolean approve = Boolean.TRUE.equals(data.get("approve"));
            String remark = (String) data.get("remark");
            return ResponseDto.success(adminService.auditMerchantApplication(username, id, approve, remark));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/activities")
    public ResponseDto getActivities(@RequestParam(required = false) String auditStatus,
                                     HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getActivities(username, auditStatus));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/activities/{id}/audit")
    public ResponseDto auditActivity(@PathVariable Long id,
                                     @RequestBody Map<String, Object> data,
                                     HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            boolean approve = Boolean.TRUE.equals(data.get("approve"));
            String remark = data.get("remark") != null ? data.get("remark").toString() : null;
            return ResponseDto.success(adminService.auditActivity(username, id, approve, remark));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/posts/pending")
    public ResponseDto getPendingPosts(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getPendingPosts(username, page, size));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/posts/{id}/audit")
    public ResponseDto auditPost(@PathVariable Long id,
                                 @RequestBody Map<String, Object> data,
                                 HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            boolean pass = Boolean.TRUE.equals(data.get("pass"));
            return ResponseDto.success(adminService.auditPost(username, id, pass));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/users")
    public ResponseDto getUsers(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) String keyword,
                                HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getUsers(username, page, size, keyword));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/users/{id}/ban")
    public ResponseDto banUser(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            adminService.banUser(username, id);
            return ResponseDto.success("User disabled successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/users/{id}/unban")
    public ResponseDto unbanUser(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            adminService.unbanUser(username, id);
            return ResponseDto.success("User restored successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/official")
    public ResponseDto getOfficialContents(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getOfficialContents(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/official")
    public ResponseDto createOfficialContent(@RequestBody Map<String, Object> data,
                                             HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.createOfficialContent(username, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/official/{id}")
    public ResponseDto deleteOfficialContent(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            adminService.deleteOfficialContent(username, id);
            return ResponseDto.success("Deleted successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/admins")
    public ResponseDto getAdmins(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getAdmins(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/admins")
    public ResponseDto createAdmin(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.createAdmin(username, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/admins/{id}")
    public ResponseDto updateAdmin(@PathVariable Long id,
                                   @RequestBody Map<String, Object> data,
                                   HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.updateAdmin(username, id, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/admins/{id}/password")
    public ResponseDto updateAdminPassword(@PathVariable Long id,
                                           @RequestBody Map<String, Object> data,
                                           HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            String newPassword = data.get("newPassword") != null ? data.get("newPassword").toString() : null;
            String confirmPassword = data.get("confirmPassword") != null ? data.get("confirmPassword").toString() : null;
            return ResponseDto.success(adminService.updateAdminPassword(username, id, newPassword, confirmPassword));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/admins/{id}")
    public ResponseDto deleteAdmin(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            adminService.deleteAdmin(username, id);
            return ResponseDto.success("Administrator deleted successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
}
