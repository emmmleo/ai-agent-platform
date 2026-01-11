package com.aiagent.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建智能体请求
 */
public class CreateAgentRequest {

    @NotBlank(message = "智能体名称不能为空")
    @Size(min = 1, max = 100, message = "智能体名称长度需1-100个字符")
    private String name;

    @Size(max = 5000, message = "描述长度不能超过5000个字符")
    private String description;

    @Size(max = 10000, message = "系统提示词长度不能超过10000个字符")
    private String systemPrompt;

    @Size(max = 5000, message = "用户提示词模板长度不能超过5000个字符")
    private String userPromptTemplate;

    private String modelConfig; // JSON字符串

    private Long workflowId;

    private String knowledgeBaseIds; // JSON字符串

    private String pluginIds; // JSON字符串

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
}
