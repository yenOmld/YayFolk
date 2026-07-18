package com.yayfolk.backend.agent.intent;

/**
 * 用户查询意图枚举。
 */
public enum Intent {
    /**
     * 资源推荐：搜索活动、帖子、非遗项目等平台资源。
     * 替代旧的 STRUCTURED_QUERY，更准确地反映功能。
     */
    RESOURCE_RECOMMEND,

    /**
     * 行程规划：按目的地和天数生成非遗旅行路线。
     */
    ITINERARY_PLANNING,

    /**
     * 知识问答：关于非遗文化、历史、工艺等知识类查询。
     */
    KNOWLEDGE_QA,

    /**
     * 闲谈/打招呼：问候、自我介绍、能力询问等，不需要调用工具。
     * 例如："你好""你是谁""你能做什么""谢谢"
     */
    CHITCHAT
}
