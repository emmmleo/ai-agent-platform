package com.aiagent.workflow.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 工作流节点实体类
 */
public class WorkflowNode {
    private String id;
    private String type;
    private String name;
    @JsonProperty("displayName")
    private String displayName;
    private Map<String, Object> data;
    private Map<String, Object> position;

    public WorkflowNode() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getPosition() {
        return position;
    }

    public void setPosition(Map<String, Object> position) {
        this.position = position;
    }
}