package com.aiagent.user.dto;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 */
public class UserProfileResponse {

    private Long id;
    private String username;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserProfileResponse(Long id, String username, String role,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

