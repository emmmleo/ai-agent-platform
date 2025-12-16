package com.aiagent.agent.dto;

import java.time.LocalDateTime;

/**
 * 智能体响应
 */
public class AgentResponse {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String systemPrompt;
    private String userPromptTemplate;
    private String modelConfig;
    private Long workflowId;
    private String knowledgeBaseIds;
    private String pluginIds;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AgentResponse() {
    }

    public AgentResponse(Long id, Long userId, String name, String description,
                        String systemPrompt, String userPromptTemplate, String modelConfig,
                        Long workflowId, String knowledgeBaseIds, String pluginIds,
                        String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.userPromptTemplate = userPromptTemplate;
        this.modelConfig = modelConfig;
        this.workflowId = workflowId;
        this.knowledgeBaseIds = knowledgeBaseIds;
        this.pluginIds = pluginIds;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPromptTemplate() {
        return userPromptTemplate;
    }

    public void setUserPromptTemplate(String userPromptTemplate) {
        this.userPromptTemplate = userPromptTemplate;
    }

    public String getModelConfig() {
        return modelConfig;
    }

    public void setModelConfig(String modelConfig) {
        this.modelConfig = modelConfig;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public String getKnowledgeBaseIds() {
        return knowledgeBaseIds;
    }

    public void setKnowledgeBaseIds(String knowledgeBaseIds) {
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    public String getPluginIds() {
        return pluginIds;
    }

    public void setPluginIds(String pluginIds) {
        this.pluginIds = pluginIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
