package com.yayfolk.backend.agent.tool;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行结果。
 */
public class ToolResult {

    private boolean success;
    private String summary;
    private Map<String, Object> data;

    public ToolResult() {
        this.data = new HashMap<>();
    }

    public static ToolResult success(String summary) {
        ToolResult result = new ToolResult();
        result.success = true;
        result.summary = summary;
        return result;
    }

    public static ToolResult success(String summary, Map<String, Object> data) {
        ToolResult result = new ToolResult();
        result.success = true;
        result.summary = summary;
        result.data = data;
        return result;
    }

    public static ToolResult fail(String summary) {
        ToolResult result = new ToolResult();
        result.success = false;
        result.summary = summary;
        return result;
    }

    public ToolResult put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    // Getters and setters

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
