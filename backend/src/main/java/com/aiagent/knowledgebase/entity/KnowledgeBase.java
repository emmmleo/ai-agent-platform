package com.aiagent.knowledgebase.entity;

import java.time.LocalDateTime;

/**
 * 知识库实体类
 */
public class KnowledgeBase {

    private Long id;
    private Long userId; // 创建者ID
    private String name; // 知识库名称
    private String description; // 知识库描述
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KnowledgeBase() {
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

