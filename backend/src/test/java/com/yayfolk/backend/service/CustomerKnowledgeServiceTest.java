package com.yayfolk.backend.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerKnowledgeServiceTest {

    private final CustomerKnowledgeService service = new CustomerKnowledgeService();

    @Test
    void testMatchRegistration() {
        String result = service.match("怎么注册");
        assertNotNull(result);
        assertTrue(result.contains("注册"));
    }

    @Test
    void testMatchActivity() {
        String result = service.match("如何参加活动");
        assertNotNull(result);
        assertTrue(result.contains("活动"));
    }

    @Test
    void testMatchMerchant() {
        String result = service.match("商家入驻");
        assertNotNull(result);
        assertTrue(result.contains("商家"));
    }

    @Test
    void testNoMatchWeather() {
        String result = service.match("今天天气怎么样");
        assertNull(result);
    }

    @Test
    void testNoMatchGreeting() {
        String result = service.match("你好");
        assertNull(result);
    }

    @Test
    void testNullInput() {
        assertNull(service.match(null));
    }

    @Test
    void testEmptyInput() {
        assertNull(service.match(""));
    }

    @Test
    void testMatchLogin() {
        String result = service.match("无法登录");
        assertNotNull(result);
        assertTrue(result.contains("登录"));
    }

    @Test
    void testMatchOrder() {
        String result = service.match("查看订单");
        assertNotNull(result);
        assertTrue(result.contains("订单"));
    }
}
