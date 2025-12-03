package com.aiagent.agent.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话列表响应。
 */
public class ChatSessionsResponse {

    private List<ChatSessionResponse> sessions = new ArrayList<>();

    public List<ChatSessionResponse> getSessions() {
        return sessions;
    }

    public void setSessions(List<ChatSessionResponse> sessions) {
        this.sessions = sessions;
    }
}
