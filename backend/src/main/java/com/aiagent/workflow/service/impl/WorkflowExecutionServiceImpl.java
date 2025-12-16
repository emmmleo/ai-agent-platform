package com.aiagent.workflow.service.impl;

import com.aiagent.workflow.dto.CreateWorkflowRequest;
import com.aiagent.workflow.dto.ExecuteWorkflowRequest;
import com.aiagent.workflow.dto.WorkflowExecutionResponse;
import com.aiagent.workflow.entity.Workflow;
import com.aiagent.workflow.entity.WorkflowExecution;
import com.aiagent.workflow.mapper.WorkflowExecutionMapper;
import com.aiagent.workflow.mapper.WorkflowMapper;
import com.aiagent.workflow.service.WorkflowExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionServiceImpl.class);

    private final WorkflowExecutionMapper executionMapper;
    private final WorkflowMapper workflowMapper;
    private final ObjectMapper objectMapper;

    public WorkflowExecutionServiceImpl(WorkflowExecutionMapper executionMapper,
                                        WorkflowMapper workflowMapper,
                                        ObjectMapper objectMapper) {
        this.executionMapper = executionMapper;
        this.workflowMapper = workflowMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public WorkflowExecutionResponse executeWorkflow(Long workflowId, Long userId, ExecuteWorkflowRequest request) {
        // 获取工作流
        Workflow workflow = workflowMapper.findById(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在");
        }

        // 创建执行记录
        LocalDateTime now = LocalDateTime.now();
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setUserId(userId);
        execution.setStatus("pending");
        
        try {
            execution.setInputParams(objectMapper.writeValueAsString(request.getInputParams()));
        } catch (Exception e) {
            log.error("序列化输入参数失败", e);
            execution.setInputParams("{}");
        }
        
        execution.setCreatedAt(now);
        executionMapper.insert(execution);

        // 异步执行工作流
        executeWorkflowAsync(execution.getId(), workflow, request.getInputParams());

        return toResponse(execution);
    }

    @Override
    public List<WorkflowExecutionResponse> getExecutionsByWorkflowId(Long workflowId, Long userId) {
        // 验证工作流权限
        Workflow workflow = workflowMapper.findByIdAndUserId(workflowId, userId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在或无权限访问");
        }

        List<WorkflowExecution> executions = executionMapper.findByWorkflowId(workflowId);
        return executions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowExecutionResponse> getExecutionsByUserId(Long userId) {
        List<WorkflowExecution> executions = executionMapper.findByUserId(userId);
        return executions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WorkflowExecutionResponse getExecutionById(Long id, Long userId) {
        WorkflowExecution execution = executionMapper.findById(id);
        if (execution == null) {
            throw new IllegalArgumentException("执行记录不存在");
        }
        // 验证权限（执行者或工作流所有者）
        if (!execution.getUserId().equals(userId)) {
            Workflow workflow = workflowMapper.findById(execution.getWorkflowId());
            if (workflow == null || !workflow.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权限访问此执行记录");
            }
        }
        return toResponse(execution);
    }

    /**
     * 异步执行工作流
     */
    @Async
    public void executeWorkflowAsync(Long executionId, Workflow workflow, Map<String, Object> inputParams) {
        try {
            log.info("开始执行工作流: {}, 执行ID: {}", workflow.getId(), executionId);
            
            // 更新状态为运行中
            WorkflowExecution execution = executionMapper.findById(executionId);
            execution.setStatus("running");
            execution.setStartedAt(LocalDateTime.now());
            executionMapper.update(execution);

            // 解析工作流定义
            CreateWorkflowRequest.WorkflowDefinition definition;
            try {
                definition = objectMapper.readValue(
                        workflow.getDefinition(),
                        CreateWorkflowRequest.WorkflowDefinition.class
                );
            } catch (Exception e) {
                throw new RuntimeException("解析工作流定义失败: " + e.getMessage(), e);
            }

            // 执行工作流
            Map<String, Object> result = executeWorkflowDefinition(definition, inputParams);

            // 更新执行结果
            execution = executionMapper.findById(executionId);
            execution.setStatus("completed");
            execution.setCompletedAt(LocalDateTime.now());
            try {
                execution.setOutputResult(objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.error("序列化输出结果失败", e);
                execution.setOutputResult("{}");
            }
            executionMapper.update(execution);

            log.info("工作流执行完成: {}, 执行ID: {}", workflow.getId(), executionId);
        } catch (Exception e) {
            log.error("执行工作流失败: {}, 执行ID: {}", workflow.getId(), executionId, e);
            // 更新执行状态为失败
            try {
                WorkflowExecution execution = executionMapper.findById(executionId);
                execution.setStatus("failed");
                execution.setCompletedAt(LocalDateTime.now());
                execution.setErrorMessage(e.getMessage());
                executionMapper.update(execution);
            } catch (Exception ex) {
                log.error("更新执行状态失败", ex);
            }
        }
    }

    /**
     * 执行工作流定义
     */
    private Map<String, Object> executeWorkflowDefinition(
            CreateWorkflowRequest.WorkflowDefinition definition,
            Map<String, Object> inputParams) {
        
        List<CreateWorkflowRequest.Node> nodes = definition.getNodes();
        List<CreateWorkflowRequest.Edge> edges = definition.getEdges();

        // 构建图结构
        Map<String, CreateWorkflowRequest.Node> nodeMap = new HashMap<>();
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (CreateWorkflowRequest.Node node : nodes) {
            nodeMap.put(node.getId(), node);
            graph.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }

        for (CreateWorkflowRequest.Edge edge : edges) {
            graph.get(edge.getSource()).add(edge.getTarget());
            inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
        }

        // 拓扑排序执行
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        Map<String, Object> nodeResults = new HashMap<>();
        Map<String, Object> context = new HashMap<>(inputParams != null ? inputParams : new HashMap<>());

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            CreateWorkflowRequest.Node node = nodeMap.get(nodeId);

            // 执行节点
            Map<String, Object> nodeResult = executeNode(node, context);
            nodeResults.put(nodeId, nodeResult);
            context.put(nodeId, nodeResult);

            // 更新后续节点
            for (String nextNodeId : graph.get(nodeId)) {
                inDegree.put(nextNodeId, inDegree.get(nextNodeId) - 1);
                if (inDegree.get(nextNodeId) == 0) {
                    queue.offer(nextNodeId);
                }
            }
        }

        return nodeResults;
    }

    /**
     * 执行单个节点
     */
    private Map<String, Object> executeNode(CreateWorkflowRequest.Node node, Map<String, Object> context) {
        // 关键修复：强制转为小写！
        // 这样无论数据库存的是 "START" 还是 "start"，这里都能匹配上 case "start"
        String nodeType = node.getType() == null ? "" : node.getType().toLowerCase();

        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", node.getId());
        result.put("nodeType", nodeType);
        result.put("nodeName", node.getName());

        log.info("正在执行节点: ID={}, Type={}, Name={}", node.getId(), nodeType, node.getName());

        try {
            switch (nodeType) {
                case "start":
                    result.put("output", "工作流开始");
                    log.info(">>> 起始节点执行完成");
                    break;
                case "end":
                    result.put("output", "工作流结束");
                    log.info(">>> 结束节点执行完成");
                    break;
                case "agent":
                    // TODO: 这里目前是模拟数据，未来要对接真实的 AI 模型
                    result.put("output", "智能体执行结果（模拟）");
                    log.info(">>> 智能体节点执行完成");
                    break;
                case "condition":
                    result.put("output", "条件判断结果（模拟）");
                    break;
                case "action":
                    result.put("output", "动作执行结果（模拟）");
                    break;
                default:
                    // 如果还走到这里，说明类型真的写错了
                    log.warn("未知节点类型: {}", nodeType);
                    result.put("output", "未知节点类型: " + nodeType);
            }
        } catch (Exception e) {
            log.error("节点执行出错: {}", node.getId(), e);
            result.put("error", e.getMessage());
            throw new RuntimeException("执行节点失败: " + node.getId(), e);
        }

        return result;
    }

    private WorkflowExecutionResponse toResponse(WorkflowExecution execution) {
        Object inputParams = null;
        Object outputResult = null;
        
        try {
            if (execution.getInputParams() != null) {
                inputParams = objectMapper.readValue(execution.getInputParams(), Object.class);
            }
            if (execution.getOutputResult() != null) {
                outputResult = objectMapper.readValue(execution.getOutputResult(), Object.class);
            }
        } catch (Exception e) {
            log.warn("解析执行记录数据失败: {}", execution.getId(), e);
        }

        WorkflowExecutionResponse response = new WorkflowExecutionResponse();
        response.setId(execution.getId());
        response.setWorkflowId(execution.getWorkflowId());
        response.setUserId(execution.getUserId());
        response.setStatus(execution.getStatus());
        response.setInputParams(inputParams);
        response.setOutputResult(outputResult);
        response.setErrorMessage(execution.getErrorMessage());
        response.setStartedAt(execution.getStartedAt());
        response.setCompletedAt(execution.getCompletedAt());
        response.setCreatedAt(execution.getCreatedAt());
        return response;
    }
}

