package com.aiagent.agent.dto;

import java.util.Collections;
import java.util.List;

/**
 * 测试智能体响应
 */
public class TestAgentResponse {

    private String answer;
    private List<String> pluginsUsed = Collections.emptyList();

    public TestAgentResponse() {
    }

    public TestAgentResponse(String answer) {
        this.answer = answer;
    }

    public TestAgentResponse(String answer, List<String> pluginsUsed) {
        this.answer = answer;
        this.pluginsUsed = pluginsUsed == null ? Collections.emptyList() : pluginsUsed;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getPluginsUsed() {
        return pluginsUsed;
    }

    public void setPluginsUsed(List<String> pluginsUsed) {
        this.pluginsUsed = pluginsUsed == null ? Collections.emptyList() : pluginsUsed;
    }
}

