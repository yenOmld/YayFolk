package com.yayfolk.backend.controller;

import com.alipay.api.AlipayApiException;
import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.service.AlipayPaymentService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alipay")
public class AliPayController {

    private final AlipayPaymentService alipayPaymentService;

    public AliPayController(AlipayPaymentService alipayPaymentService) {
        this.alipayPaymentService = alipayPaymentService;
    }

    @PostMapping("/notify")
    public String notifyCallback(HttpServletRequest request) throws AlipayApiException {
        return alipayPaymentService.handleAsyncNotification(readRequestParams(request));
    }

    @GetMapping("/return")
    public void returnCallback(@RequestParam(value = "out_trade_no", required = false) String outTradeNo,
                               HttpServletResponse response) throws IOException {
        response.sendRedirect(alipayPaymentService.buildReturnRedirectUrl(outTradeNo));
    }

    @PostMapping("/reconcile")
    public ResponseDto reconcileTrade(@RequestBody(required = false) Map<String, Object> data,
                                      HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            String outTradeNo = stringValue(data == null ? null : data.get("outTradeNo"));
            return ResponseDto.success(alipayPaymentService.reconcileTradeForCurrentUser(username, outTradeNo));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/refund/orders/{id}")
    public ResponseDto refundOrder(@PathVariable Long id,
                                   @RequestBody(required = false) Map<String, Object> data,
                                   HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(alipayPaymentService.refundUserOrder(username, id, stringValue(data == null ? null : data.get("reason"))));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/refund/bookings/{id}")
    public ResponseDto refundBooking(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, Object> data,
                                     HttpServletRequest request) {
        try {
            String username = requireUsername(request);
            return ResponseDto.success(alipayPaymentService.refundUserActivityBooking(username, id, stringValue(data == null ? null : data.get("reason"))));
        } catch (Exception e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    private String requireUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        if (username == null) {
            throw new RuntimeException("Unauthorized, please login first");
        }
        return username.toString();
    }

    private Map<String, String> readRequestParams(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                continue;
            }
            result.put(entry.getKey(), values[0]);
        }
        return result;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }
}