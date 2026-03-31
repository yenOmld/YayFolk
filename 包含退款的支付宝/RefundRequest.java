package org.example.java_web_01.controller.controllerImpL;

import java.util.List;

// PayRequest.java
public class RefundRequest {
    private Integer orderId;
    private String payId;   // 注意：前端传的是 payId 列表
    private String value;          // 或者 BigDecimal value（推荐）
    public String reason ;


    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getPayId() {
        return payId;
    }

    public void setPayId(String payId) {
        this.payId = payId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "RefundRequest{" +
                "orderId=" + orderId +
                ", payId='" + payId + '\'' +
                ", value='" + value + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}