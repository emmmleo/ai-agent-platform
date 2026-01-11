package com.aiagent.agent.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 智能体对话请求
 */
public class ChatRequest {

    @NotBlank(message = "问题不能为空")
    private String question;
    private Long sessionId;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}

