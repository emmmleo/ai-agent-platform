package com.aiagent.agent.dto;

/**
 * 智能体对话响应
 */
public class ChatResponse {

    private String answer;
    private String source; // 回答来源：direct/rag/workflow
    private java.util.List<String> pluginsUsed; // 调用的插件名称列表
    private Long sessionId;
    private RagContext ragContext;

    public ChatResponse() {
    }

    public ChatResponse(String answer, String source, java.util.List<String> pluginsUsed, Long sessionId) {
        this.answer = answer;
        this.source = source;
        this.pluginsUsed = pluginsUsed;
        this.sessionId = sessionId;
    }

    public ChatResponse(String answer, String source, java.util.List<String> pluginsUsed, Long sessionId, RagContext ragContext) {
        this.answer = answer;
        this.source = source;
        this.pluginsUsed = pluginsUsed;
        this.sessionId = sessionId;
        this.ragContext = ragContext;
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

    public RagContext getRagContext() {
        return ragContext;
    }

    public void setRagContext(RagContext ragContext) {
        this.ragContext = ragContext;
    }

    /**
     * RAG 检索上下文信息
     */
    public static class RagContext {
        private boolean success;
        private String message;
        private java.util.List<RagReference> references;

        public RagContext() {}

        public RagContext(boolean success, String message, java.util.List<RagReference> references) {
            this.success = success;
            this.message = message;
            this.references = references;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public java.util.List<RagReference> getReferences() {
            return references;
        }

        public void setReferences(java.util.List<RagReference> references) {
            this.references = references;
        }
    }

    /**
     * RAG 引用片段
     */
    public static class RagReference {
        private Long knowledgeBaseId;
        private Long documentId;
        private String content;
        private Double score;

        public RagReference() {}

        public RagReference(Long knowledgeBaseId, Long documentId, String content, Double score) {
            this.knowledgeBaseId = knowledgeBaseId;
            this.documentId = documentId;
            this.content = content;
            this.score = score;
        }

        public Long getKnowledgeBaseId() {
            return knowledgeBaseId;
        }

        public void setKnowledgeBaseId(Long knowledgeBaseId) {
            this.knowledgeBaseId = knowledgeBaseId;
        }

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }
}

