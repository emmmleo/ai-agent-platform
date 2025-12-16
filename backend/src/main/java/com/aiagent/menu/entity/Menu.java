package com.aiagent.menu.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Menu {

    private Long id;
    private Long parentId;
    private String title;
    private String path;
    private Integer orderNum;
    private String allowedRoles;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(String allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public List<String> getAllowedRoleList() {
        if (allowedRoles == null || allowedRoles.isBlank()) {
            return Collections.emptyList();
        }
        String[] parts = allowedRoles.split(",");
        List<String> roles = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                roles.add(part.trim());
            }
        }
        return roles;
    }
}

