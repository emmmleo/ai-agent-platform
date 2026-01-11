package com.aiagent.workflow.dto;

import java.time.LocalDateTime;

/**
 * 工作流响应
 */
public class WorkflowResponse {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Object definition; // JSON对象
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WorkflowResponse() {
    }

    public WorkflowResponse(Long id, Long userId, String name, String description,
                           Object definition, String status,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.definition = definition;
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

    public Object getDefinition() {
        return definition;
    }

    public void setDefinition(Object definition) {
        this.definition = definition;
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

