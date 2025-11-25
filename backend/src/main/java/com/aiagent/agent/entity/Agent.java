package com.aiagent.agent.entity;

import java.time.LocalDateTime;

/**
 * 智能体实体类
 */
public class Agent {

    private Long id;
    private Long userId; // 创建者ID（需要添加到数据库表）
    private String name; // 智能体名称
    private String description; // 描述
    private String systemPrompt; // 系统提示词
    private String userPromptTemplate; // 用户提示词模板
    private String modelConfig; // 模型配置（JSON字符串）
    private Long workflowId; // 关联工作流ID
    private String knowledgeBaseIds; // 关联知识库ID列表（JSON字符串）
    private String pluginIds; // 关联插件ID列表（JSON字符串）
    private String status; // 状态：draft, published
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Agent() {
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
