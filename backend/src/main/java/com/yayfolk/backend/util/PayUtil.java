package com.yayfolk.backend.util;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.alibaba.fastjson.JSONObject;
import com.yayfolk.backend.config.AlipayProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PayUtil {

    private final AlipayProperties alipayProperties;

    public PayUtil(AlipayProperties alipayProperties) {
        this.alipayProperties = alipayProperties;
    }

    public String sendRequestToAlipay(String outTradeNo, BigDecimal totalAmount, String subject) throws AlipayApiException {
        return sendRequestToAlipay(outTradeNo, totalAmount, subject, null, null);
    }

    public String sendRequestToAlipay(String outTradeNo,
                                      BigDecimal totalAmount,
                                      String subject,
                                      String body,
                                      String returnUrl) throws AlipayApiException {
        assertConfigured();
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(StringUtils.hasText(returnUrl) ? returnUrl : alipayProperties.getReturnUrl());
        request.setNotifyUrl(alipayProperties.getNotifyUrl());

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("body", StringUtils.hasText(body) ? body : "");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toJSONString());

        return createClient().pageExecute(request).getBody();
    }

    public String query(String outTradeNo) throws AlipayApiException {
        assertConfigured();
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        request.setBizContent(bizContent.toJSONString());
        AlipayTradeQueryResponse response = createClient().execute(request);
        return response.getBody();
    }

    public AlipayTradeRefundResponse refund(String outTradeNo,
                                            BigDecimal refundAmount,
                                            String refundReason,
                                            String outRequestNo) throws AlipayApiException {
        assertConfigured();
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(outTradeNo);
        model.setRefundAmount(refundAmount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        if (StringUtils.hasText(refundReason)) {
            model.setRefundReason(refundReason);
        }
        if (StringUtils.hasText(outRequestNo)) {
            model.setOutRequestNo(outRequestNo);
        }
        request.setBizModel(model);
        return createClient().execute(request);
    }

    public boolean verifyNotify(Map<String, String> params) throws AlipayApiException {
        assertConfigured();
        return AlipaySignature.rsaCheckV1(
                params,
                alipayProperties.getPublicKey(),
                alipayProperties.getCharset(),
                alipayProperties.getSignType()
        );
    }

    public AlipayConfig getAlipayConfig() {
        assertConfigured();
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl(alipayProperties.getGatewayUrl());
        config.setAppId(alipayProperties.getAppId());
        config.setPrivateKey(alipayProperties.getPrivateKey());
        config.setFormat(alipayProperties.getFormat());
        config.setAlipayPublicKey(alipayProperties.getPublicKey());
        config.setCharset(alipayProperties.getCharset());
        config.setSignType(alipayProperties.getSignType());
        return config;
    }

    private AlipayClient createClient() throws AlipayApiException {
        return new DefaultAlipayClient(getAlipayConfig());
    }

    private void assertConfigured() {
        if (!alipayProperties.isConfigured()) {
            throw new RuntimeException("Alipay configuration is incomplete");
        }
    }
}
