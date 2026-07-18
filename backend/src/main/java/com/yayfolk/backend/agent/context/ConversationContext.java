package com.yayfolk.backend.agent.context;

import java.util.*;

/**
 * 对话上下文，用于在多轮对话中保持记忆。
 * 替代之前无状态的 agent 调用方式。
 */
public class ConversationContext {

    private final List<Message> messages;
    private final int maxHistory;

    public ConversationContext() {
        this(10);
    }

    public ConversationContext(int maxHistory) {
        this.messages = new ArrayList<>();
        this.maxHistory = maxHistory;
    }

    /**
     * 从历史消息列表构建上下文。
     * @param history 历史消息，每条包含 "role" 和 "content"
     */
    public static ConversationContext fromHistory(List<Map<String, String>> history) {
        ConversationContext ctx = new ConversationContext();
        if (history != null) {
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if (role != null && content != null) {
                    ctx.add(role, content);
                }
            }
        }
        return ctx;
    }

    /**
     * 从原始消息列表构建上下文（用于 ExploreMessage 转换）。
     */
    public static ConversationContext fromExploreMessages(
            List<?> messages, int maxHistory) {
        ConversationContext ctx = new ConversationContext(maxHistory);
        if (messages == null) return ctx;

        // 只取最近 N 条
        int start = Math.max(0, messages.size() - maxHistory);
        for (int i = start; i < messages.size(); i++) {
            Object msg = messages.get(i);
            try {
                // 使用反射获取 role 和 content 字段
                String role = (String) msg.getClass().getMethod("getRole").invoke(msg);
                String content = (String) msg.getClass().getMethod("getContent").invoke(msg);
                if (role != null && content != null) {
                    ctx.add(role, content);
                }
            } catch (Exception ignored) {
                // 反射失败则跳过
            }
        }
        return ctx;
    }

    public void add(String role, String content) {
        messages.add(new Message(role, content));
        // 保持历史长度在限制之内
        while (messages.size() > maxHistory) {
            messages.remove(0);
        }
    }

    public void addUser(String content) {
        add("user", content);
    }

    public void addAssistant(String content) {
        add("assistant", content);
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public int size() {
        return messages.size();
    }

    /**
     * 构建一个用于 LLM prompt 的对话历史摘要字符串。
     * 格式：用户：xxx\n助手：xxx\n...
     */
    public String toPromptHistory() {
        if (messages.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("【对话历史】\n");
        int idx = 1;
        for (Message msg : messages) {
            if ("user".equals(msg.getRole())) {
                sb.append("用户").append(idx).append("：").append(msg.getContent()).append("\n");
                idx++;
            } else if ("assistant".equals(msg.getRole())) {
                sb.append("助手：").append(msg.getContent()).append("\n");
            }
        }
        sb.append("---\n");
        return sb.toString();
    }

    /**
     * 获取用户最近一次提问的内容。
     */
    public String getLastUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                return messages.get(i).getContent();
            }
        }
        return null;
    }

    /**
     * 获取助手最近一次回复的内容。
     */
    public String getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("assistant".equals(messages.get(i).getRole())) {
                return messages.get(i).getContent();
            }
        }
        return null;
    }

    /**
     * 判断用户当前输入是否为对上一轮结果的追问（如"第一个在哪里""还有更多吗"）。
     */
    public boolean isFollowUpQuestion(String currentInput) {
        if (messages.isEmpty()) return false;
        String[] followUpPatterns = {
                "第一个", "第二个", "第三个", "上一个", "下一个",
                "还有吗", "还有更多", "更多", "其他的", "别的",
                "在哪里", "什么时候", "多少钱", "怎么报名",
                "详细", "具体", "介绍一下", "说说"
        };
        for (String pattern : followUpPatterns) {
            if (currentInput.contains(pattern)) return true;
        }
        return false;
    }

    /**
     * 获取上一轮 agent 返回的资源卡片摘要（从 assistant 消息中提取）。
     */
    public String getPreviousResourcesSummary() {
        String lastAssistant = getLastAssistantMessage();
        if (lastAssistant == null) return "";
        // 截取前 500 字符作为上一轮的资源上下文
        if (lastAssistant.length() > 500) {
            return lastAssistant.substring(0, 500) + "...";
        }
        return lastAssistant;
    }

    public static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}
