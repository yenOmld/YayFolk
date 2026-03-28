package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.MerchantService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final MerchantService merchantService;

    public OrderController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    private String requireUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        if (username == null) {
            throw new RuntimeException("Unauthorized, please login first");
        }
        return username.toString();
    }

    @PostMapping
    public ResponseDto createOrder(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.createOrder(username, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping
    public ResponseDto getMyOrders(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMyOrdersAsUser(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/overview")
    public ResponseDto getMyOrderOverview(HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMyOrderOverview(username));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseDto cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            merchantService.cancelOrder(username, id);
            return ResponseDto.success("Order cancelled successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseDto cancelActivityBooking(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            merchantService.cancelActivityBooking(username, id);
            return ResponseDto.success("Activity booking cancelled successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseDto deleteActivityBooking(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            merchantService.deleteActivityBooking(username, id);
            return ResponseDto.success("Activity booking deleted successfully");
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/activities/{activityId}/book")
    public ResponseDto bookActivity(@PathVariable Long activityId,
                                    @RequestBody Map<String, Object> data,
                                    HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.bookActivity(username, activityId, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
}
