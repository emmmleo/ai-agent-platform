package com.aiagent.agent.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话历史响应。
 */
public class ChatHistoryResponse {

    private List<ChatHistoryMessage> messages = new ArrayList<>();

    public List<ChatHistoryMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatHistoryMessage> messages) {
        this.messages = messages;
    }

    public static class ChatHistoryMessage {
        private String type;
        private String content;

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
    }
}
