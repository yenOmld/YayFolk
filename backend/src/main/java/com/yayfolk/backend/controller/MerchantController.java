package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.AlipayPaymentService;
import com.yayfolk.backend.service.MerchantService;
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
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantService merchantService;
    private final AlipayPaymentService alipayPaymentService;

    public MerchantController(MerchantService merchantService,
                              AlipayPaymentService alipayPaymentService) {
        this.merchantService = merchantService;
        this.alipayPaymentService = alipayPaymentService;
    }

    private String requireUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        if (username == null) {
            throw new RuntimeException("Unauthorized, please login first");
        }
        return username.toString();
    }

    @PostMapping("/apply")
    public ResponseDto apply(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.applyMerchant(username, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/apply/status")
    public ResponseDto applyStatus(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMyApplication(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/activities")
    public ResponseDto getActivities(@RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer size,
                                     HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            if (page != null || size != null) {
                return ResponseDto.success(merchantService.getMyActivitiesPage(username, page, size));
            }
            return ResponseDto.success(merchantService.getMyActivities(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/activities")
    public ResponseDto createActivity(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.createActivity(username, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/activities/{id}")
    public ResponseDto updateActivity(@PathVariable Long id,
                                      @RequestBody Map<String, Object> data,
                                      HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.updateActivity(username, id, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/activities/{id}")
    public ResponseDto deleteActivity(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            merchantService.deleteActivity(username, id);
            return ResponseDto.success("Deleted successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/activities/{id}/bookings")
    public ResponseDto getBookings(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getActivityBookings(username, id));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/bookings")
    public ResponseDto getMerchantBookings(@RequestParam(required = false) Long activityId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size,
                                           HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMerchantBookings(username, activityId, status, keyword, page, size));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/bookings/{id}")
    public ResponseDto getMerchantBookingDetail(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMerchantBookingDetail(username, id));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/bookings/lookup")
    public ResponseDto lookupBooking(@RequestParam String code, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.lookupBookingForCheckin(username, code));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/bookings/lookup-image")
    public ResponseDto lookupBookingByImage(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.lookupBookingForCheckinImage(username, String.valueOf(data.get("imageData"))));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/bookings/{id}/checkin")
    public ResponseDto checkin(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.checkinBooking(username, id));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/bookings/{id}/refund")
    public ResponseDto refundBooking(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, Object> data,
                                     HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            String reason = data == null || data.get("reason") == null ? null : String.valueOf(data.get("reason")).trim();
            return ResponseDto.success(alipayPaymentService.refundMerchantActivityBooking(username, id, reason));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/bookings/{id}/reject")
    public ResponseDto rejectBooking(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, Object> data,
                                     HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            String reason = data == null || data.get("reason") == null ? null : String.valueOf(data.get("reason")).trim();
            return ResponseDto.success(alipayPaymentService.rejectMerchantActivityBooking(username, id, reason));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/products")
    public ResponseDto getProducts(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMyProducts(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/products")
    public ResponseDto createProduct(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.createProduct(username, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PutMapping("/products/{id}")
    public ResponseDto updateProduct(@PathVariable Long id,
                                     @RequestBody Map<String, Object> data,
                                     HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.updateProduct(username, id, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseDto deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            merchantService.deleteProduct(username, id);
            return ResponseDto.success("Deleted successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/orders")
    public ResponseDto getOrders(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMyOrders(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseDto getMerchantStats(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMerchantStats(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
}
