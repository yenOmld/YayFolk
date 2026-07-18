package com.yayfolk.backend.agent.tool;

import java.util.Map;

/**
 * Agent 工具接口。
 * 每个工具自描述，供 LLM function calling 使用。
 */
public interface Tool {

    /**
     * 工具名称（供 LLM 调用时使用）。
     */
    String getName();

    /**
     * 工具描述（供 LLM 理解工具用途）。
     */
    String getDescription();

    /**
     * 工具参数 JSON Schema（供 LLM 了解参数格式）。
     */
    Map<String, Object> getParametersSchema();

    /**
     * 执行工具，传入参数，返回结果。
     */
    ToolResult execute(Map<String, Object> parameters);
}
