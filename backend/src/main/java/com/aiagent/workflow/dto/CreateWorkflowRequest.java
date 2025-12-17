package com.aiagent.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 创建工作流请求
 */
public class CreateWorkflowRequest {

    @NotBlank(message = "工作流名称不能为空")
    @Size(min = 1, max = 100, message = "工作流名称长度需1-100个字符")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;

    @NotNull(message = "工作流定义不能为空")
    @JsonProperty("definition")
    private WorkflowDefinition definition;

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

    public WorkflowDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(WorkflowDefinition definition) {
        this.definition = definition;
    }

    /**
     * 工作流定义（节点和边）
     */
    public static class WorkflowDefinition {
        private List<Node> nodes;
        private List<Edge> edges;

        public List<Node> getNodes() {
            return nodes;
        }

        public void setNodes(List<Node> nodes) {
            this.nodes = nodes;
        }

        public List<Edge> getEdges() {
            return edges;
        }

        public void setEdges(List<Edge> edges) {
            this.edges = edges;
        }
    }

    /**
     * 节点定义
     */
    public static class Node {
        private String id;
        private String type; // start/end/agent/condition/action
        private String name;
        @JsonProperty("data")
        private Map<String, Object> data;
        private Position position;

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

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }
    }

    /**
     * 边定义
     */
    public static class Edge {
        private String id;
        private String source; // 源节点ID
        private String target; // 目标节点ID
        private String condition; // 条件（用于条件节点）

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

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }
    }

    /**
     * 节点位置
     */
    public static class Position {
        private Integer x;
        private Integer y;

        public Integer getX() {
            return x;
        }

        public void setX(Integer x) {
            this.x = x;
        }

        public Integer getY() {
            return y;
        }

        public void setY(Integer y) {
            this.y = y;
        }
    }
}

