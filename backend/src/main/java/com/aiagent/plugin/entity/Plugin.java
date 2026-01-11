package com.aiagent.plugin.entity;

import java.time.LocalDateTime;

/**
 * 插件实体类
 */
public class Plugin {

    private Long id;
    private Long userId; // 创建者ID
    private String name; // 插件名称
    private String description; // 插件描述
    private String openapiSpec; // OpenAPI规范（JSON字符串）
    private Boolean enabled; // 是否启用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Plugin() {
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

    public String getOpenapiSpec() {
        return openapiSpec;
    }

    public void setOpenapiSpec(String openapiSpec) {
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

