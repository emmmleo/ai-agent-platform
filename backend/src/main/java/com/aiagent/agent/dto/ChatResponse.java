package com.aiagent.agent.dto;

/**
 * 智能体对话响应
 */
public class ChatResponse {

    private String answer;
    private String source; // 回答来源：direct/rag/workflow

    public ChatResponse() {
    }

    public ChatResponse(String answer, String source) {
        this.answer = answer;
        this.source = source;
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
}

