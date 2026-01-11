package com.aiagent.menu.dto;

import java.util.ArrayList;
import java.util.List;

public class MenuResponse {

    private Long id;
    private String title;
    private String path;
    private List<MenuResponse> children = new ArrayList<>();

    public MenuResponse() {
    }

    public MenuResponse(Long id, String title, String path) {
        this.id = id;
        this.title = title;
        this.path = path;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<MenuResponse> getChildren() {
        return children;
    }

    public void setChildren(List<MenuResponse> children) {
        this.children = children;
    }
}

