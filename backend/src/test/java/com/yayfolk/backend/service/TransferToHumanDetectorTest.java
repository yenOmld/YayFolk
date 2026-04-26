package com.yayfolk.backend.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferToHumanDetectorTest {

    private final TransferToHumanDetector detector = new TransferToHumanDetector();

    @Test
    void testTransferKeyword() {
        assertTrue(detector.shouldTransfer("转人工"));
    }

    @Test
    void testFindCustomerService() {
        assertTrue(detector.shouldTransfer("我要找客服"));
    }

    @Test
    void testHelpMeTransfer() {
        assertTrue(detector.shouldTransfer("帮我转人工客服"));
    }

    @Test
    void testGreeting() {
        assertFalse(detector.shouldTransfer("你好"));
    }

    @Test
    void testRegistrationQuestion() {
        assertFalse(detector.shouldTransfer("怎么注册"));
    }

    @Test
    void testNullInput() {
        assertFalse(detector.shouldTransfer(null));
    }

    @Test
    void testEmptyInput() {
        assertFalse(detector.shouldTransfer(""));
    }

    @Test
    void testHumanService() {
        assertTrue(detector.shouldTransfer("人工服务"));
    }

    @Test
    void testNoRobot() {
        assertTrue(detector.shouldTransfer("不要机器人"));
    }
}
