package com.aiagent.workflow.service.impl;

import com.aiagent.workflow.dto.CreateWorkflowRequest;
import com.aiagent.workflow.dto.WorkflowResponse;
import com.aiagent.workflow.entity.Workflow;
import com.aiagent.workflow.mapper.WorkflowMapper;
import com.aiagent.workflow.service.WorkflowService;
import com.aiagent.workflow.util.DAGValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowServiceImpl.class);
    private static final String DEFAULT_STATUS = "draft";

    private final WorkflowMapper workflowMapper;
    private final ObjectMapper objectMapper;

    public WorkflowServiceImpl(WorkflowMapper workflowMapper, ObjectMapper objectMapper) {
        this.workflowMapper = workflowMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public WorkflowResponse createWorkflow(Long userId, CreateWorkflowRequest request) {
        // 验证DAG
        DAGValidator.validate(request.getDefinition());

        // 将定义转换为JSON字符串
        String definitionJson;
        try {
            definitionJson = objectMapper.writeValueAsString(request.getDefinition());
        } catch (Exception e) {
            log.error("序列化工作流定义失败", e);
            throw new IllegalArgumentException("工作流定义格式错误: " + e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        Workflow workflow = new Workflow();
        workflow.setUserId(userId);
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setDefinition(definitionJson);
        workflow.setStatus(DEFAULT_STATUS);
        workflow.setCreatedAt(now);
        workflow.setUpdatedAt(now);

        workflowMapper.insert(workflow);

        return toResponse(workflow);
    }

    @Override
    public List<WorkflowResponse> getWorkflowsByUserId(Long userId) {
        List<Workflow> workflows = workflowMapper.findByUserId(userId);
        return workflows.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WorkflowResponse getWorkflowById(Long id, Long userId) {
        Workflow workflow = workflowMapper.findByIdAndUserId(id, userId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在或无权限访问");
        }
        return toResponse(workflow);
    }

    @Override
    @Transactional
    public WorkflowResponse updateWorkflow(Long id, Long userId, CreateWorkflowRequest request) {
        // 验证DAG
        DAGValidator.validate(request.getDefinition());

        Workflow workflow = workflowMapper.findByIdAndUserId(id, userId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在或无权限访问");
        }

        // 将定义转换为JSON字符串
        String definitionJson;
        try {
            definitionJson = objectMapper.writeValueAsString(request.getDefinition());
        } catch (Exception e) {
            log.error("序列化工作流定义失败", e);
            throw new IllegalArgumentException("工作流定义格式错误: " + e.getMessage());
        }

        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setDefinition(definitionJson);
        workflow.setUpdatedAt(LocalDateTime.now());

        workflowMapper.update(workflow);

        return toResponse(workflow);
    }

    @Override
    @Transactional
    public void deleteWorkflow(Long id, Long userId) {
        Workflow workflow = workflowMapper.findByIdAndUserId(id, userId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在或无权限访问");
        }
        workflowMapper.deleteById(id);
    }

    private WorkflowResponse toResponse(Workflow workflow) {
        // 将JSON字符串解析为对象
        Object definition = null;
        try {
            definition = objectMapper.readValue(workflow.getDefinition(), Object.class);
        } catch (Exception e) {
            log.warn("解析工作流定义失败: {}", workflow.getId(), e);
        }

        return new WorkflowResponse(
                workflow.getId(),
                workflow.getUserId(),
                workflow.getName(),
                workflow.getDescription(),
                definition,
                workflow.getStatus(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }
}

