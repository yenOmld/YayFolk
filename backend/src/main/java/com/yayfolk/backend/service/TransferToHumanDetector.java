package com.yayfolk.backend.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TransferToHumanDetector {

    private static final List<String> TRANSFER_KEYWORDS = Arrays.asList(
            "转人工",
            "我要找客服",
            "人工客服",
            "真人客服",
            "不想和机器人说话",
            "不想和ai说话",
            "不想和AI说话",
            "找真人",
            "转真人",
            "人工服务",
            "真人服务",
            "联系客服",
            "找客服",
            "人工回复",
            "转接人工",
            "帮我转人工",
            "能不能转人工",
            "可以转人工吗",
            "我要人工",
            "不要机器人"
    );

    public boolean shouldTransfer(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }
        String input = userInput.trim().toLowerCase();
        for (String keyword : TRANSFER_KEYWORDS) {
            if (input.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
