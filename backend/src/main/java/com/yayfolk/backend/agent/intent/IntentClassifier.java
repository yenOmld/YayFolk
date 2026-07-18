package com.yayfolk.backend.agent.intent;

import com.yayfolk.backend.agent.context.ConversationContext;

/**
 * 意图分类器接口。
 */
public interface IntentClassifier {

    /**
     * 根据用户输入分类意图。
     * @param userInput 用户自然语言输入
     * @return 分类后的意图，默认返回 KNOWLEDGE_QA
     */
    Intent classify(String userInput);

    /**
     * 根据用户输入和对话上下文分类意图。
     * 默认实现忽略上下文，子类可覆盖以利用历史信息提升准确率。
     *
     * @param userInput 用户自然语言输入
     * @param ctx       对话上下文（历史消息）
     * @return 分类后的意图
     */
    default Intent classify(String userInput, ConversationContext ctx) {
        return classify(userInput);
    }
}
