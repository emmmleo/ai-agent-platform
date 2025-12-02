package com.aiagent.agent.dto;

/**
 * 智能体对话响应
 */
public class ChatResponse {

    private String answer;
    private String source; // 回答来源：direct/rag/workflow
    private java.util.List<String> pluginsUsed; // 调用的插件名称列表
    private Long sessionId;

    public ChatResponse() {
    }

    public ChatResponse(String answer, String source, java.util.List<String> pluginsUsed, Long sessionId) {
        this.answer = answer;
        this.source = source;
        this.pluginsUsed = pluginsUsed;
        this.sessionId = sessionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public java.util.List<String> getPluginsUsed() {
        return pluginsUsed;
    }

    public void setPluginsUsed(java.util.List<String> pluginsUsed) {
        this.pluginsUsed = pluginsUsed;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}

