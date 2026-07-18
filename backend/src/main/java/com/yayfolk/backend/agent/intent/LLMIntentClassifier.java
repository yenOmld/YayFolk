package com.yayfolk.backend.agent.intent;

import com.yayfolk.backend.agent.context.ConversationContext;
import com.yayfolk.backend.ai.client.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 基于 LLM（DeepSeek）的意图分类器。
 * 使用 LLM 理解用户自然语言，准确映射到三个意图。
 * 支持对话上下文，能理解追问场景。
 * 失败时降级到 KeywordIntentClassifier。
 */
@Primary
@Component
public class LLMIntentClassifier implements IntentClassifier {

    private static final Logger logger = LoggerFactory.getLogger(LLMIntentClassifier.class);

    private final DeepSeekClient deepSeekClient;
    private final KeywordIntentClassifier fallback;

    public LLMIntentClassifier(DeepSeekClient deepSeekClient, KeywordIntentClassifier fallback) {
        this.deepSeekClient = deepSeekClient;
        this.fallback = fallback;
    }

    @Override
    public Intent classify(String userInput) {
        return classifyWithContext(userInput, null);
    }

    @Override
    public Intent classify(String userInput, ConversationContext ctx) {
        return classifyWithContext(userInput, ctx);
    }

    private Intent classifyWithContext(String userInput, ConversationContext ctx) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return Intent.KNOWLEDGE_QA;
        }

        try {
            String systemPrompt = buildSystemPrompt(ctx);
            String prompt = buildUserPrompt(userInput, ctx);

            String result = deepSeekClient.chat(systemPrompt, prompt);

            if (result != null) {
                String trimmed = result.trim().toUpperCase();
                if (trimmed.contains("CHITCHAT")) {
                    return Intent.CHITCHAT;
                } else if (trimmed.contains("RESOURCE_RECOMMEND") || trimmed.contains("RESOURCE")) {
                    return Intent.RESOURCE_RECOMMEND;
                } else if (trimmed.contains("ITINERARY_PLANNING") || trimmed.contains("ITINERARY")) {
                    return Intent.ITINERARY_PLANNING;
                } else if (trimmed.contains("KNOWLEDGE_QA") || trimmed.contains("KNOWLEDGE")) {
                    return Intent.KNOWLEDGE_QA;
                }
            }
        } catch (Exception e) {
            logger.warn("LLM intent classification failed, falling back to keyword classifier", e);
        }

        // 降级到关键词分类器
        return fallback.classify(userInput);
    }

    private String buildSystemPrompt(ConversationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个意图分类助手。根据用户输入，返回以下四个意图之一：\n");
        sb.append("\n");
        sb.append("- CHITCHAT：问候、自我介绍、道别、感谢、询问AI能力、无意义短句等。\n");
        sb.append("  例如：「你好」「你是谁」「你能做什么」「谢谢」「再见」「在吗」\n");
        sb.append("  注意：这类消息不需要搜索平台资源，直接聊天回复即可！\n");
        sb.append("\n");
        sb.append("- RESOURCE_RECOMMEND：用户想要搜索、查找、推荐活动、帖子、非遗项目等平台资源。\n");
        sb.append("  例如：「推荐北京的非遗活动」「有什么刺绣相关的帖子」「有没有免费活动」「找3个苏州的活动」\n");
        sb.append("\n");
        sb.append("- ITINERARY_PLANNING：用户想要规划旅行路线、行程安排。\n");
        sb.append("  例如：「帮我规划3天苏州之旅」「南京2日游路线」「怎么玩」\n");
        sb.append("\n");
        sb.append("- KNOWLEDGE_QA：用户询问非遗知识、历史文化、工艺特点等。\n");
        sb.append("  例如：「什么是刺绣」「京剧的历史」「剪纸怎么做」「介绍一下苏绣」\n");
        sb.append("\n");
        sb.append("重要规则：\n");
        sb.append("1. 只返回意图名称，不要返回任何其他内容\n");
        sb.append("2. 首先判断是否为 CHITCHAT：问候、自我介绍、道别说 CHITCHAT\n");
        sb.append("3. 用户说「推荐」「有什么」「帮我找」「有哪些」→ RESOURCE_RECOMMEND\n");
        sb.append("4. 用户说「规划」「行程」「攻略」「之旅」「几日游」→ ITINERARY_PLANNING\n");
        sb.append("5. 用户说「是什么」「介绍」「历史」「特点」「怎么做」→ KNOWLEDGE_QA\n");
        sb.append("6. 「你是谁」「你叫什么」「你能做什么」→ 必须返回 CHITCHAT，不要返回 KNOWLEDGE_QA！\n");

        // 上下文提示
        if (ctx != null && !ctx.isEmpty()) {
            sb.append("\n");
            sb.append("【对话历史】\n");
            sb.append(ctx.toPromptHistory());
            sb.append("注意：如果用户当前问题是关于上一轮结果的追问"
                    + "（如「第一个在哪里」「还有更多吗」「详细说说」），"
                    + "请保持与上一轮相同的意图。\n");
        }

        return sb.toString();
    }

    private String buildUserPrompt(String userInput, ConversationContext ctx) {
        if (ctx != null && ctx.isFollowUpQuestion(userInput)) {
            return "用户追问（请参考对话历史理解上下文）：" + userInput;
        }
        return userInput;
    }
}
