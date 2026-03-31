package com.yayfolk.backend.service;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.yayfolk.backend.config.AlipayProperties;
import com.yayfolk.backend.entity.ActivityReserve;
import com.yayfolk.backend.entity.Order;
import com.yayfolk.backend.util.PayUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AlipayPaymentService {

    private final MerchantService merchantService;
    private final PayUtil payUtil;
    private final AlipayProperties alipayProperties;

    public AlipayPaymentService(MerchantService merchantService,
                                PayUtil payUtil,
                                AlipayProperties alipayProperties) {
        this.merchantService = merchantService;
        this.payUtil = payUtil;
        this.alipayProperties = alipayProperties;
    }

    public Map<String, Object> createOrderPayment(String username, Long orderId) throws AlipayApiException {
        Order order = merchantService.prepareUserOrderForExternalPayment(username, orderId, "alipay");
        if ("paid".equalsIgnoreCase(trim(order.getStatus()))) {
            return merchantService.getMyOrderDetail(username, orderId);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("paymentMode", "alipay_page");
        result.put("paymentType", "alipay");
        result.put("businessType", "order");
        result.put("businessId", order.getId());
        result.put("outTradeNo", order.getOrderNo());
        result.put("payAmount", order.getPayAmount());
        result.put("formHtml", payUtil.sendRequestToAlipay(
                order.getOrderNo(),
                toAmount(order.getPayAmount()),
                buildOrderSubject(order),
                buildOrderBody(order),
                alipayProperties.getReturnUrl()
        ));
        return result;
    }

    public Map<String, Object> createBookingPayment(String username, Long bookingId) throws AlipayApiException {
        ActivityReserve booking = merchantService.prepareUserBookingForExternalPayment(username, bookingId, "alipay");
        if (Integer.valueOf(1).equals(booking.getPayStatus())) {
            return merchantService.getMyActivityBookingDetail(username, bookingId);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("paymentMode", "alipay_page");
        result.put("paymentType", "alipay");
        result.put("businessType", "booking");
        result.put("businessId", booking.getId());
        result.put("outTradeNo", booking.getReserveNo());
        result.put("payAmount", booking.getPayAmount());
        result.put("formHtml", payUtil.sendRequestToAlipay(
                booking.getReserveNo(),
                toAmount(booking.getPayAmount()),
                buildBookingSubject(booking),
                buildBookingBody(booking),
                alipayProperties.getReturnUrl()
        ));
        return result;
    }

    public String handleAsyncNotification(Map<String, String> params) throws AlipayApiException {
        if (!payUtil.verifyNotify(params)) {
            return "failure";
        }
        String tradeStatus = trim(params.get("trade_status"));
        if (!"TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) && !"TRADE_FINISHED".equalsIgnoreCase(tradeStatus)) {
            return "success";
        }
        String outTradeNo = trim(params.get("out_trade_no"));
        if (!StringUtils.hasText(outTradeNo)) {
            return "failure";
        }
        merchantService.confirmPaidByTradeNo(outTradeNo, "alipay", parseAlipayTime(params.get("gmt_payment")));
        return "success";
    }

    public String buildReturnRedirectUrl(String outTradeNo) {
        String frontendBaseUrl = trimTrailingSlash(alipayProperties.getFrontendBaseUrl());
        try {
            reconcilePaymentOnReturn(outTradeNo);
            if (isOrderTradeNo(outTradeNo)) {
                Order order = merchantService.findOrderByOrderNo(outTradeNo);
                String paymentState = "paid".equalsIgnoreCase(trim(order.getStatus())) ? "success" : "pending";
                return frontendBaseUrl + "/personal/orders?payment=" + paymentState + "&orderId=" + order.getId() + "&tradeNo=" + outTradeNo;
            }
            ActivityReserve booking = merchantService.findBookingByReserveNo(outTradeNo);
            String paymentState = Integer.valueOf(1).equals(booking.getPayStatus()) ? "success" : "pending";
            return frontendBaseUrl + "/personal/activities/" + booking.getId() + "/pay-result?payment=" + paymentState + "&tradeNo=" + outTradeNo;
        } catch (Exception ignored) {
            return frontendBaseUrl + "/personal/orders?payment=pending";
        }
    }

    public Map<String, Object> reconcileTradeForCurrentUser(String username, String outTradeNo) throws AlipayApiException {
        String tradeNo = trim(outTradeNo);
        if (!StringUtils.hasText(tradeNo)) {
            throw new RuntimeException("Trade number cannot be empty");
        }
        reconcilePaymentOnReturn(tradeNo);
        if (isOrderTradeNo(tradeNo)) {
            Order order = merchantService.findOrderByOrderNo(tradeNo);
            return merchantService.getMyOrderDetail(username, order.getId());
        }
        ActivityReserve booking = merchantService.findBookingByReserveNo(tradeNo);
        return merchantService.getMyActivityBookingDetail(username, booking.getId());
    }

    public Map<String, Object> refundUserOrder(String username, Long orderId, String reason) throws AlipayApiException {
        Order order = merchantService.prepareUserOrderForRefund(username, orderId);
        if ("refunded".equalsIgnoreCase(trim(order.getStatus()))) {
            return merchantService.getMyOrderDetail(username, orderId);
        }
        ensureAlipayTrade(order.getPaymentType());
        AlipayTradeRefundResponse response = payUtil.refund(
                order.getOrderNo(),
                toAmount(order.getPayAmount()),
                normalizeRefundReason(reason, "User requested refund"),
                buildRefundRequestNo(order.getOrderNo())
        );
        if (!response.isSuccess()) {
            throw new RuntimeException(firstText(response.getSubMsg(), response.getMsg(), "Alipay refund failed"));
        }
        merchantService.confirmOrderRefunded(username, orderId, new Date());
        return merchantService.getMyOrderDetail(username, orderId);
    }

    public Map<String, Object> refundUserActivityBooking(String username, Long bookingId, String reason) throws AlipayApiException {
        ActivityReserve booking = merchantService.prepareUserBookingForRefund(username, bookingId);
        if (Integer.valueOf(2).equals(booking.getPayStatus())) {
            return merchantService.getMyActivityBookingDetail(username, bookingId);
        }
        ensureAlipayTrade(booking.getPaymentType());
        AlipayTradeRefundResponse response = payUtil.refund(
                booking.getReserveNo(),
                toAmount(booking.getPayAmount()),
                normalizeRefundReason(reason, "User cancelled booking"),
                buildRefundRequestNo(booking.getReserveNo())
        );
        if (!response.isSuccess()) {
            throw new RuntimeException(firstText(response.getSubMsg(), response.getMsg(), "Alipay refund failed"));
        }
        merchantService.confirmBookingRefunded(username, bookingId, new Date(), normalizeRefundReason(reason, "User cancelled booking"));
        return merchantService.getMyActivityBookingDetail(username, bookingId);
    }

    public Map<String, Object> refundMerchantActivityBooking(String username, Long bookingId, String reason) throws AlipayApiException {
        ActivityReserve booking = merchantService.prepareMerchantBookingForRefund(username, bookingId);
        if (Integer.valueOf(2).equals(booking.getPayStatus())) {
            return merchantService.getMerchantBookingDetail(username, bookingId);
        }
        ensureAlipayTrade(booking.getPaymentType());
        AlipayTradeRefundResponse response = payUtil.refund(
                booking.getReserveNo(),
                toAmount(booking.getPayAmount()),
                normalizeRefundReason(reason, "Merchant refunded booking"),
                buildRefundRequestNo(booking.getReserveNo())
        );
        if (!response.isSuccess()) {
            throw new RuntimeException(firstText(response.getSubMsg(), response.getMsg(), "Alipay refund failed"));
        }
        merchantService.confirmMerchantBookingRefunded(
                username,
                bookingId,
                new Date(),
                normalizeRefundReason(reason, "Merchant refunded booking"),
                true
        );
        return merchantService.getMerchantBookingDetail(username, bookingId);
    }

    public Map<String, Object> rejectMerchantActivityBooking(String username, Long bookingId, String reason) throws AlipayApiException {
        ActivityReserve booking = merchantService.prepareMerchantBookingForReject(username, bookingId);
        if (Integer.valueOf(1).equals(booking.getPayStatus()) && booking.getPayAmount() != null && booking.getPayAmount() > 0) {
            ensureAlipayTrade(booking.getPaymentType());
            AlipayTradeRefundResponse response = payUtil.refund(
                    booking.getReserveNo(),
                    toAmount(booking.getPayAmount()),
                    normalizeRefundReason(reason, "Merchant rejected booking"),
                    buildRefundRequestNo(booking.getReserveNo())
            );
            if (!response.isSuccess()) {
                throw new RuntimeException(firstText(response.getSubMsg(), response.getMsg(), "Alipay refund failed"));
            }
            merchantService.confirmMerchantBookingRefunded(
                    username,
                    bookingId,
                    new Date(),
                    normalizeRefundReason(reason, "Merchant rejected booking"),
                    false
            );
            return merchantService.getMerchantBookingDetail(username, bookingId);
        }
        merchantService.confirmMerchantBookingRejected(
                username,
                bookingId,
                new Date(),
                normalizeRefundReason(reason, "Merchant rejected booking")
        );
        return merchantService.getMerchantBookingDetail(username, bookingId);
    }

    private BigDecimal toAmount(Integer amountInCent) {
        return BigDecimal.valueOf(amountInCent == null ? 0L : amountInCent.longValue())
                .divide(BigDecimal.valueOf(100L), 2, BigDecimal.ROUND_HALF_UP);
    }

    private String buildOrderSubject(Order order) {
        return truncate(firstText(order.getProductName(), "YayFolk order"), 128);
    }

    private String buildOrderBody(Order order) {
        return truncate("order:" + trim(order.getOrderNo()), 120);
    }

    private String buildBookingSubject(ActivityReserve booking) {
        return truncate(firstText(booking.getActivityTitle(), "YayFolk activity booking"), 128);
    }

    private String buildBookingBody(ActivityReserve booking) {
        return truncate("booking:" + trim(booking.getReserveNo()), 120);
    }

    private void reconcilePaymentOnReturn(String outTradeNo) {
        String tradeNo = trim(outTradeNo);
        if (!StringUtils.hasText(tradeNo)) {
            return;
        }
        try {
            JSONObject payload = JSONObject.parseObject(payUtil.query(tradeNo));
            JSONObject response = payload == null ? null : payload.getJSONObject("alipay_trade_query_response");
            if (response == null) {
                return;
            }
            String code = trim(response.getString("code"));
            String tradeStatus = trim(response.getString("trade_status"));
            if (!"10000".equals(code)) {
                return;
            }
            if (!"TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) && !"TRADE_FINISHED".equalsIgnoreCase(tradeStatus)) {
                return;
            }
            merchantService.confirmPaidByTradeNo(
                    tradeNo,
                    "alipay",
                    parseAlipayTime(firstText(response.getString("send_pay_date"), response.getString("gmt_payment")))
            );
        } catch (Exception ignored) {
        }
    }

    private Date parseAlipayTime(String value) {
        if (!StringUtils.hasText(value)) {
            return new Date();
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value.trim());
        } catch (Exception ignored) {
            return new Date();
        }
    }

    private void ensureAlipayTrade(String paymentType) {
        if (!isAlipayTrade(paymentType)) {
            throw new RuntimeException("This record was not paid by Alipay");
        }
    }

    private boolean isAlipayTrade(String paymentType) {
        return "alipay".equalsIgnoreCase(trim(paymentType));
    }

    private boolean isOrderTradeNo(String outTradeNo) {
        return trim(outTradeNo).startsWith("YF");
    }

    private String buildRefundRequestNo(String outTradeNo) {
        return truncate(outTradeNo + "R" + System.currentTimeMillis(), 64);
    }

    private String normalizeRefundReason(String reason, String fallback) {
        String value = trim(reason);
        return StringUtils.hasText(value) ? truncate(value, 80) : fallback;
    }

    private String trimTrailingSlash(String value) {
        String text = trim(value);
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        String text = trim(value);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String firstText(String first, String second) {
        return firstText(first, second, "");
    }

    private String firstText(String first, String second, String fallback) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return fallback;
    }
}