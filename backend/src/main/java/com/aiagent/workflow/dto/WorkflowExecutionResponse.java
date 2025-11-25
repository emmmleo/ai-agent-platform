package com.aiagent.workflow.dto;

import java.time.LocalDateTime;

/**
 * 工作流执行响应
 */
public class WorkflowExecutionResponse {

    private Long id;
    private Long workflowId;
    private Long userId;
    private String status; // pending/running/completed/failed
    private Object inputParams; // JSON对象
    private Object outputResult; // JSON对象
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public WorkflowExecutionResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getInputParams() {
        return inputParams;
    }

    public void setInputParams(Object inputParams) {
        this.inputParams = inputParams;
    }

    public Object getOutputResult() {
        return outputResult;
    }

    public void setOutputResult(Object outputResult) {
        this.outputResult = outputResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

