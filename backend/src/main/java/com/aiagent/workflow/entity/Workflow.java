package com.aiagent.workflow.entity;

import java.time.LocalDateTime;

/**
 * 工作流实体类
 */
public class Workflow {

    private Long id;
    private Long userId; // 创建者ID
    private String name; // 工作流名称
    private String description; // 工作流描述
    private String definition; // 工作流定义（JSON字符串）
    private String status; // 状态：draft/published
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Workflow() {
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

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
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
    // Transient fields for runtime usage (not persisted directly)
    private transient java.util.List<WorkflowNode> nodes;
    private transient java.util.List<WorkflowEdge> edges;

    public java.util.List<WorkflowNode> getNodes() {
        if (nodes == null && definition != null) {
            parseDefinition();
        }
        return nodes != null ? nodes : java.util.Collections.emptyList();
    }

    public void setNodes(java.util.List<WorkflowNode> nodes) {
        this.nodes = nodes;
    }

    public java.util.List<WorkflowEdge> getEdges() {
        if (edges == null && definition != null) {
            parseDefinition();
        }
        return edges != null ? edges : java.util.Collections.emptyList();
    }

    public void setEdges(java.util.List<WorkflowEdge> edges) {
        this.edges = edges;
    }

    private void parseDefinition() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // Assuming definition is a JSON object with "nodes" and "edges" arrays
            // But we need to know the structure. DTO uses "definition": { nodes: [], edges: [] }
            // Let's assume definition String IS the Definition object JSON.
            // Check CreateWorkflowRequest structure: Definition has nodes/edges.
            Definition def = mapper.readValue(definition, Definition.class);
            this.nodes = def.nodes;
            this.edges = def.edges;
        } catch (Exception e) {
            // Log or ignore? Entity shouldn't log usually.
            // e.printStackTrace();
            this.nodes = java.util.Collections.emptyList();
            this.edges = java.util.Collections.emptyList();
        }
    }

    private static class Definition {
        public java.util.List<WorkflowNode> nodes;
        public java.util.List<WorkflowEdge> edges;
    }
}

