package com.aiagent.workflow.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 工作流边实体类
 */
public class WorkflowEdge {
    private String id;
    private String source;
    private String target;
    private String type;
    private Map<String, Object> data;

    public WorkflowEdge() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}