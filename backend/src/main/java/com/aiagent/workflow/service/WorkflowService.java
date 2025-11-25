package com.aiagent.workflow.service;

import com.aiagent.workflow.dto.CreateWorkflowRequest;
import com.aiagent.workflow.dto.WorkflowResponse;

import java.util.List;

/**
 * 工作流服务接口
 */
public interface WorkflowService {

    /**
     * 创建工作流
     */
    WorkflowResponse createWorkflow(Long userId, CreateWorkflowRequest request);

    /**
     * 获取用户的所有工作流
     */
    List<WorkflowResponse> getWorkflowsByUserId(Long userId);

    /**
     * 根据ID获取工作流详情
     */
    WorkflowResponse getWorkflowById(Long id, Long userId);

    /**
     * 更新工作流
     */
    WorkflowResponse updateWorkflow(Long id, Long userId, CreateWorkflowRequest request);

    /**
     * 删除工作流
     */
    void deleteWorkflow(Long id, Long userId);
}

