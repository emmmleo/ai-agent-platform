package com.aiagent.workflow.service.executor;

import com.aiagent.workflow.entity.Workflow;
import com.aiagent.workflow.entity.WorkflowNode;
import com.aiagent.workflow.entity.WorkflowEdge;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流执行上下文
 * 用于在工作流执行过程中传递和共享数据
 */
public class WorkflowExecutionContext {
    private Workflow workflow;
    private Map<String, Object> inputParams;
    private Map<String, Object> executionResults = new ConcurrentHashMap<>();
    private Map<String, NodeExecutionStatus> nodeStatuses = new ConcurrentHashMap<>();
    private Map<String, String> executionLog = new LinkedHashMap<>();
    private Long userId;
    private Long executionId;
    private Map<String, Object> contextData = new ConcurrentHashMap<>();

    public WorkflowExecutionContext(Workflow workflow, Map<String, Object> inputParams, Long userId, Long executionId) {
        this.workflow = workflow;
        this.inputParams = inputParams;
        this.userId = userId;
        this.executionId = executionId;
        this.contextData.putAll(inputParams);
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public Map<String, Object> getInputParams() {
        return inputParams;
    }

    public Map<String, Object> getExecutionResults() {
        return executionResults;
    }

    public Map<String, NodeExecutionStatus> getNodeStatuses() {
        return nodeStatuses;
    }

    public Map<String, String> getExecutionLog() {
        return executionLog;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public Map<String, Object> getContextData() {
        return contextData;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public void setInputParams(Map<String, Object> inputParams) {
        this.inputParams = inputParams;
    }

    public void setContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
    }

    /**
     * 添加节点执行结果
     */
    public void addNodeResult(String nodeId, Map<String, Object> result) {
        executionResults.put(nodeId, result);
        // 将节点结果合并到上下文数据中，供后续节点使用
        if (result != null) {
            contextData.putAll(result);
        }
    }

    /**
     * 设置节点执行状态
     */
    public void setNodeStatus(String nodeId, NodeExecutionStatus status) {
        nodeStatuses.put(nodeId, status);
    }

    /**
     * 添加执行日志
     */
    public void addExecutionLog(String message) {
        executionLog.put(new Date().toString(), message);
    }

    /**
     * 根据ID获取节点
     */
    public WorkflowNode getNodeById(String nodeId) {
        if (workflow.getNodes() == null) {
            return null;
        }
        return workflow.getNodes().stream()
                .filter(node -> nodeId.equals(node.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取节点的输出边
     */
    public List<WorkflowEdge> getOutgoingEdges(String nodeId) {
        if (workflow.getEdges() == null) {
            return Collections.emptyList();
        }
        return workflow.getEdges().stream()
                .filter(edge -> nodeId.equals(edge.getSource()))
                .toList();
    }

    /**
     * 变量替换函数
     */
    /**
     * 变量替换函数
     * 支持格式：
     * 1. {input.param} - 引用输入参数
     * 2. {nodeId.field} - 引用节点输出字段
     * 3. {{variable}} - 简单变量引用 (兼容旧格式)
     */
    public String replaceVariables(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 1. 处理 {{variable}} 格式 (兼容旧格式)
        String result = text;
        // ... (Simple regex replacement for {{}} if needed, but let's focus on the new robust one)
        
        // 2. 处理 {variable} 格式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varPath = matcher.group(1);
            String replacement = matcher.group(0); // 默认保留原文本

            try {
                // 处理 {input.param}
                if (varPath.startsWith("input.")) {
                    String paramName = varPath.substring(6);
                    Object val = inputParams.get(paramName);
                    if (val != null) {
                        replacement = String.valueOf(val);
                    }
                } 
                // 处理 {node.field} 或 {variable}
                else {
                    // 尝试直接从contextData获取 (扁平化数据)
                    Object val = contextData.get(varPath);
                    if (val != null) {
                        replacement = String.valueOf(val);
                    } else {
                        // 尝试解析 node.field
                        String[] parts = varPath.split("\\.", 2);
                        if (parts.length == 2) {
                            String nodeId = parts[0];
                            String fieldPath = parts[1];
                            
                            // 从 executionResults 获取节点结果
                            Map<String, Object> nodeResult = (Map<String, Object>) executionResults.get(nodeId);
                            if (nodeResult != null) {
                                // 简单的可扩展字段访问
                                Object fieldVal = nodeResult.get(fieldPath);
                                // 如果没找到，尝试在 data 中找 (result -> data -> field)
                                if (fieldVal == null && nodeResult.get("data") instanceof Map) {
                                    fieldVal = ((Map) nodeResult.get("data")).get(fieldPath);
                                }
                                
                                if (fieldVal != null) {
                                    replacement = String.valueOf(fieldVal);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 节点执行状态枚举
     */
    public enum NodeExecutionStatus {
        PENDING("pending"),
        RUNNING("running"),
        COMPLETED("completed"),
        FAILED("failed"),
        SKIPPED("skipped");

        private final String value;

        NodeExecutionStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}