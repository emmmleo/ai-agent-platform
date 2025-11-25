package com.aiagent.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 测试智能体请求
 */
public class TestAgentRequest {

    @NotBlank(message = "测试问题不能为空")
    @Size(min = 1, max = 1000, message = "测试问题长度需1-1000个字符")
    private String question;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}

