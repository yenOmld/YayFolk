package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.AlipayPaymentService;
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
    private final AlipayPaymentService alipayPaymentService;

    public OrderController(MerchantService merchantService,
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

    private boolean isAlipayRequest(Map<String, Object> data) {
        if (data == null) {
            return false;
        }
        Object paymentType = data.get("paymentType");
        return paymentType != null && "alipay".equalsIgnoreCase(String.valueOf(paymentType).trim());
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

    @GetMapping("/{id}")
    public ResponseDto getOrderDetail(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMyOrderDetail(username, id));
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

    @PostMapping("/{id}/pay")
    public ResponseDto payOrder(@PathVariable Long id,
                                @RequestBody(required = false) Map<String, Object> data,
                                HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            if (isAlipayRequest(data)) {
                return ResponseDto.success(alipayPaymentService.createOrderPayment(username, id));
            }
            return ResponseDto.success(merchantService.payForOrder(username, id, data));
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
            Map<String, Object> detail = merchantService.getMyActivityBookingDetail(username, id);
            String paymentStatus = detail.get("paymentStatus") == null ? "" : String.valueOf(detail.get("paymentStatus"));
            String paymentType = detail.get("paymentType") == null ? "" : String.valueOf(detail.get("paymentType"));
            if ("paid".equalsIgnoreCase(paymentStatus) && "alipay".equalsIgnoreCase(paymentType)) {
                alipayPaymentService.refundUserActivityBooking(username, id, "User cancelled booking");
                return ResponseDto.success("Activity booking cancelled successfully");
            }
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

    @GetMapping("/bookings/{id}")
    public ResponseDto getActivityBookingDetail(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getMyActivityBookingDetail(username, id));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/bookings/{id}/pay")
    public ResponseDto payActivityBooking(@PathVariable Long id,
                                          @RequestBody(required = false) Map<String, Object> data,
                                          HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            if (isAlipayRequest(data)) {
                return ResponseDto.success(alipayPaymentService.createBookingPayment(username, id));
            }
            return ResponseDto.success(merchantService.payForActivityBooking(username, id, data));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/bookings/{id}/qrcode")
    public ResponseDto getActivityBookingQrCode(@PathVariable Long id, HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.getActivityBookingQrCode(username, id));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/bookings/{id}/review")
    public ResponseDto submitActivityBookingReview(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> data,
                                                   HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(merchantService.submitActivityBookingReview(username, id, data));
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
