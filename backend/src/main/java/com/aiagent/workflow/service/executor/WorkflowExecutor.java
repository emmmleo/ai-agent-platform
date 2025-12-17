package com.aiagent.workflow.service.executor;

import com.aiagent.workflow.entity.Workflow;
import com.aiagent.workflow.entity.WorkflowEdge;
import com.aiagent.workflow.entity.WorkflowExecution;
import com.aiagent.workflow.entity.WorkflowNode;
import com.aiagent.workflow.service.WorkflowExecutionService;
import com.aiagent.workflow.util.DAGValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 工作流执行引擎
 */
@Service
public class WorkflowExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutor.class);

    @Autowired
    private NodeExecutorFactory nodeExecutorFactory;

    @Autowired
    private WorkflowExecutionService workflowExecutionService;

    @Autowired
    private DAGValidator dagValidator;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 执行工作流
     */
    public Map<String, Object> executeWorkflow(Workflow workflow, Map<String, Object> inputParams, Long userId) {
        logger.info("开始执行工作流: {}", workflow.getName());

        // 1. 验证工作流是否为DAG
        if (!dagValidator.isDAG(workflow)) {
            throw new IllegalArgumentException("工作流不是有效的DAG结构，存在循环依赖");
        }

        // 2. 生成执行序列（拓扑排序）
        List<String> executionOrder = dagValidator.topologicalSort(workflow);
        if (executionOrder.isEmpty()) {
            throw new IllegalArgumentException("工作流没有可执行的节点序列");
        }

        logger.info("工作流执行序列: {}", executionOrder);

        // 3. 创建执行记录
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflow.getId());
        execution.setUserId(userId);
        execution.setStatus("running");
        execution.setInputParams(convertToJson(inputParams));
        execution.setStartedAt(LocalDateTime.now());
        execution.setCreatedAt(LocalDateTime.now());

        Long executionId = workflowExecutionService.createExecution(execution);
        execution.setId(executionId);

        // 4. 创建执行上下文
        WorkflowExecutionContext context = new WorkflowExecutionContext(workflow, inputParams, userId, executionId);
        context.addExecutionLog("工作流开始执行");

        // 5. 按顺序执行节点
        try {
            for (String nodeId : executionOrder) {
                WorkflowNode node = context.getNodeById(nodeId);
                if (node == null) {
                    throw new IllegalArgumentException("节点不存在: " + nodeId);
                }

                // 检查是否需要跳过该节点（基于边的条件）
                if (shouldSkipNode(nodeId, context)) {
                    context.setNodeStatus(nodeId, WorkflowExecutionContext.NodeExecutionStatus.SKIPPED);
                    context.addExecutionLog("跳过节点: " + node.getName());
                    continue;
                }

                context.setNodeStatus(nodeId, WorkflowExecutionContext.NodeExecutionStatus.RUNNING);
                context.addExecutionLog("开始执行节点: " + node.getName());

                // 获取节点执行器
                NodeExecutor executor = nodeExecutorFactory.getExecutor(node.getType());
                if (executor == null) {
                    throw new IllegalArgumentException("不支持的节点类型: " + node.getType());
                }

                // 执行节点
                Map<String, Object> nodeResult = executor.execute(node, context).join();

                // 记录节点执行结果
                context.addNodeResult(nodeId, nodeResult);
                context.setNodeStatus(nodeId, WorkflowExecutionContext.NodeExecutionStatus.COMPLETED);
                context.addExecutionLog("节点执行完成: " + node.getName());

                // 检查是否需要提前终止工作流
                if (shouldTerminateWorkflow(node, nodeResult)) {
                    context.addExecutionLog("工作流提前终止");
                    break;
                }
            }

            // 6. 工作流执行完成
            execution.setStatus("completed");
            execution.setOutputResult(convertToJson(context.getExecutionResults()));
            context.addExecutionLog("工作流执行完成");

        } catch (Exception e) {
            logger.error("工作流执行失败: {}", e.getMessage(), e);
            execution.setStatus("failed");
            execution.setErrorMessage(e.getMessage());
            execution.setOutputResult(convertToJson(context.getExecutionResults()));
            context.addExecutionLog("工作流执行失败: " + e.getMessage());
        } finally {
            // 更新执行记录
            execution.setCompletedAt(LocalDateTime.now());
            workflowExecutionService.updateExecution(execution);
        }

        return context.getExecutionResults();
    }

    /**
     * 检查是否需要跳过节点
     */
    private boolean shouldSkipNode(String nodeId, WorkflowExecutionContext context) {
        // 获取到达该节点的所有入边
        List<WorkflowNode> predecessorNodes = getPredecessorNodes(nodeId, context);
        
        // 检查是否所有前驱节点都已执行完成
        for (WorkflowNode predecessor : predecessorNodes) {
            WorkflowExecutionContext.NodeExecutionStatus status = context.getNodeStatuses().get(predecessor.getId());
            if (status != WorkflowExecutionContext.NodeExecutionStatus.COMPLETED) {
                return true;
            }
        }

        // 检查边的条件
        List<WorkflowEdge> incomingEdges = getIncomingEdges(nodeId, context);
        for (WorkflowEdge edge : incomingEdges) {
            if (!checkEdgeCondition(edge, context)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取节点的前驱节点
     */
    private List<WorkflowNode> getPredecessorNodes(String nodeId, WorkflowExecutionContext context) {
        List<WorkflowNode> predecessors = new ArrayList<>();
        if (context.getWorkflow().getEdges() == null) {
            return predecessors;
        }

        for (WorkflowEdge edge : context.getWorkflow().getEdges()) {
            if (nodeId.equals(edge.getTarget())) {
                WorkflowNode sourceNode = context.getNodeById(edge.getSource());
                if (sourceNode != null) {
                    predecessors.add(sourceNode);
                }
            }
        }
        return predecessors;
    }

    /**
     * 获取节点的入边
     */
    private List<WorkflowEdge> getIncomingEdges(String nodeId, WorkflowExecutionContext context) {
        List<WorkflowEdge> incomingEdges = new ArrayList<>();
        if (context.getWorkflow().getEdges() == null) {
            return incomingEdges;
        }

        for (WorkflowEdge edge : context.getWorkflow().getEdges()) {
            if (nodeId.equals(edge.getTarget())) {
                incomingEdges.add(edge);
            }
        }
        return incomingEdges;
    }

    /**
     * 检查边的条件
     */
    private boolean checkEdgeCondition(WorkflowEdge edge, WorkflowExecutionContext context) {
        Map<String, Object> data = edge.getData();
        if (data == null || !data.containsKey("condition")) {
            return true; // 没有条件，默认通过
        }

        String condition = (String) data.get("condition");
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        // 这里可以实现更复杂的条件表达式解析和求值
        // 简单示例：支持"variable == value"格式
        try {
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                if (parts.length == 2) {
                    String variable = parts[0].trim();
                    String value = parts[1].trim().replaceAll("'", "");
                    
                    Object contextValue = context.getContextData().get(variable);
                    return contextValue != null && contextValue.toString().equals(value);
                }
            }
        } catch (Exception e) {
            logger.warn("条件解析失败: {}", condition, e);
            return true; // 解析失败，默认通过
        }

        return true;
    }

    /**
     * 检查是否需要提前终止工作流
     */
    private boolean shouldTerminateWorkflow(WorkflowNode node, Map<String, Object> result) {
        // 检查节点类型是否为终止节点
        return "end".equals(node.getType());
    }

    /**
     * 将Map转换为JSON字符串
     */
    private String convertToJson(Map<String, Object> map) {
        // 这里可以使用Jackson或其他JSON库
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            logger.error("JSON转换失败: {}", e.getMessage(), e);
            return "{}";
        }
    }
}
