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
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
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

    private String getRequiredString(Map<String, Object> data, String key, String message) {
        Object value = data.get(key);
        if (value == null) {
            throw new RuntimeException(message);
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            throw new RuntimeException(message);
        }
        return text;
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

    @GetMapping("/posts")
    public ResponseDto getPosts(@RequestParam(required = false) String auditStatus,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getPosts(username, auditStatus, page, size));
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
            String remark = data.get("remark") != null ? data.get("remark").toString() : null;
            return ResponseDto.success(adminService.auditPost(username, id, pass, remark));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/posts/batch-audit")
    public ResponseDto batchAuditPosts(@RequestBody Map<String, Object> data,
                                       HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            Object idsObj = data.get("ids");
            if (!(idsObj instanceof List)) {
                return ResponseDto.error(400, "Invalid ids");
            }
            List<Long> ids = new ArrayList<Long>();
            for (Object item : (List<?>) idsObj) {
                if (item instanceof Number) {
                    ids.add(((Number) item).longValue());
                } else if (item != null) {
                    ids.add(Long.parseLong(item.toString()));
                }
            }
            boolean pass = Boolean.TRUE.equals(data.get("pass"));
            String remark = data.get("remark") != null ? data.get("remark").toString() : null;
            return ResponseDto.success(adminService.batchAuditPosts(username, ids, pass, remark));
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
    public ResponseDto banUser(@PathVariable Long id,
                               @RequestBody(required = false) Map<String, Object> data,
                               HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            String reason = data != null && data.get("reason") != null ? data.get("reason").toString() : null;
            adminService.banUser(username, id, reason);
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

    @GetMapping("/users/unban-applications")
    public ResponseDto getUnbanApplications(@RequestParam(required = false) String status,
                                            HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getUnbanApplications(username, status));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/users/unban-applications/{id}/audit")
    public ResponseDto auditUnbanApplication(@PathVariable Long id,
                                             @RequestBody Map<String, Object> data,
                                             HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            boolean approve = Boolean.TRUE.equals(data.get("approve"));
            String remark = data.get("remark") != null ? data.get("remark").toString() : null;
            return ResponseDto.success(adminService.auditUnbanApplication(username, id, approve, remark));
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
    @GetMapping("/official/activities")
    public ResponseDto getOfficialActivities(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getOfficialActivities(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/official/activities")
    public ResponseDto createOfficialActivity(@RequestBody Map<String, Object> data,
                                              HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.createOfficialActivity(username, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/official/activities/{id}")
    public ResponseDto updateOfficialActivity(@PathVariable Long id,
                                              @RequestBody Map<String, Object> data,
                                              HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.updateOfficialActivity(username, id, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/official/activities/{id}")
    public ResponseDto deleteOfficialActivity(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            adminService.deleteOfficialActivity(username, id);
            return ResponseDto.success("Activity deleted successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/official/heritages")
    public ResponseDto getOfficialHeritages(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getOfficialHeritages(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/official/heritages")
    public ResponseDto createOfficialHeritage(@RequestBody Map<String, Object> data,
                                              HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.createOfficialHeritage(username, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/official/heritages/{id}")
    public ResponseDto updateOfficialHeritage(@PathVariable Long id,
                                              @RequestBody Map<String, Object> data,
                                              HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.updateOfficialHeritage(username, id, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/official/heritages/{id}")
    public ResponseDto deleteOfficialHeritage(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            adminService.deleteOfficialHeritage(username, id);
            return ResponseDto.success("Heritage deleted successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
    // Official content management - get works list (top 20 by heat)
    @GetMapping("/official/works")
    public ResponseDto getOfficialWorks(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getOfficialWorks(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/official/works")
    public ResponseDto createOfficialWork(@RequestBody Map<String, Object> data,
                                          HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.createOfficialWork(username, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/official/works/{id}")
    public ResponseDto updateOfficialWork(@PathVariable Long id,
                                          @RequestBody Map<String, Object> data,
                                          HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.updateOfficialWork(username, id, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/official/works/{id}")
    public ResponseDto deleteOfficialWork(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            adminService.deleteOfficialWork(username, id);
            return ResponseDto.success("Work deleted successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/official/published")
    public ResponseDto getHomepagePublishedState(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(adminService.getHomepagePublishedState(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    // Official content management - publish to homepage
    @PostMapping("/official/publish")
    public ResponseDto publishToHomepage(@RequestBody Map<String, Object> data,
                                          HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            String type = getRequiredString(data, "type", "Type is required");
            Object idsObj = data.get("ids");
            if (!(idsObj instanceof List)) {
                return ResponseDto.error(400, "Invalid ids");
            }
            List<Long> ids = new ArrayList<Long>();
            for (Object item : (List<?>) idsObj) {
                if (item instanceof Number) {
                    ids.add(((Number) item).longValue());
                } else if (item != null) {
                    ids.add(Long.parseLong(item.toString()));
                }
            }
            return ResponseDto.success(adminService.publishToHomepage(username, type, ids));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
}



