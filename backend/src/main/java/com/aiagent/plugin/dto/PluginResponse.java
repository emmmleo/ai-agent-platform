package com.aiagent.plugin.dto;

import java.time.LocalDateTime;

/**
 * 插件响应
 */
public class PluginResponse {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Object openapiSpec; // JSON对象
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PluginResponse() {
    }

    public PluginResponse(Long id, Long userId, String name, String description,
                         Object openapiSpec, Boolean enabled,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.openapiSpec = openapiSpec;
        this.enabled = enabled;
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

    public Object getOpenapiSpec() {
        return openapiSpec;
    }

    public void setOpenapiSpec(Object openapiSpec) {
        this.openapiSpec = openapiSpec;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

