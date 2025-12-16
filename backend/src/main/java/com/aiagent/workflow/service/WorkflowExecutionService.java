package com.aiagent.workflow.service;

import com.aiagent.workflow.dto.ExecuteWorkflowRequest;
import com.aiagent.workflow.dto.WorkflowExecutionResponse;

import java.util.List;

/**
 * 工作流执行服务接口
 */
public interface WorkflowExecutionService {

    /**
     * 执行工作流
     */
    WorkflowExecutionResponse executeWorkflow(Long workflowId, Long userId, ExecuteWorkflowRequest request);

    /**
     * 获取工作流的执行记录
     */
    List<WorkflowExecutionResponse> getExecutionsByWorkflowId(Long workflowId, Long userId);

    /**
     * 获取用户的执行记录
     */
    List<WorkflowExecutionResponse> getExecutionsByUserId(Long userId);

    /**
     * 根据ID获取执行记录详情
     */
    WorkflowExecutionResponse getExecutionById(Long id, Long userId);
}

