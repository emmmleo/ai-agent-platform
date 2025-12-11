package com.aiagent.agent.dto;

import java.util.ArrayList;
import java.util.List;
import com.aiagent.agent.dto.ChatResponse.RagContext;

/**
 * 对话历史响应。
 */
public class ChatHistoryResponse {

    private List<ChatHistoryMessage> messages = new ArrayList<>();
    private Long sessionId;

    public List<ChatHistoryMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatHistoryMessage> messages) {
        this.messages = messages;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public static class ChatHistoryMessage {
        private String type;
        private String content;
        private List<String> plugins;
        private RagContext ragContext;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<String> getPlugins() {
            return plugins;
        }

        public void setPlugins(List<String> plugins) {
            this.plugins = plugins;
        }

        public RagContext getRagContext() {
            return ragContext;
        }

        public void setRagContext(RagContext ragContext) {
            this.ragContext = ragContext;
        }
    }
}
