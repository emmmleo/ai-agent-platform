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

import com.aiagent.agent.client.DeepSeekClient;
import org.springframework.beans.factory.ObjectProvider;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Service
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionServiceImpl.class);

    private final WorkflowExecutionMapper executionMapper;
    private final WorkflowMapper workflowMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<DeepSeekClient> deepSeekClientProvider;

    public WorkflowExecutionServiceImpl(WorkflowExecutionMapper executionMapper,
                                        WorkflowMapper workflowMapper,
                                        ObjectMapper objectMapper,
                                        ObjectProvider<DeepSeekClient> deepSeekClientProvider) {
        this.executionMapper = executionMapper;
        this.workflowMapper = workflowMapper;
        this.objectMapper = objectMapper;
        this.deepSeekClientProvider = deepSeekClientProvider;
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

    @Override
    @Transactional
    public Long createExecution(WorkflowExecution execution) {
        executionMapper.insert(execution);
        return execution.getId();
    }

    @Override
    @Transactional
    public void updateExecution(WorkflowExecution execution) {
        executionMapper.update(execution);
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
            // For condition nodes, we need to determine which branch to follow
            String activeBranchId = null;
            if ("condition".equalsIgnoreCase(node.getType())) {
                activeBranchId = (String) nodeResult.get("activeBranchId");
            }

            for (String nextNodeId : graph.get(nodeId)) {
                // Check if this edge should be traversed
                boolean shouldTraverse = true;
                
                if (activeBranchId != null) {
                    // Find the edge connecting nodeId to nextNodeId
                    // We need to look up the edge definition to check its condition/branchId
                    // Since graph structure doesn't store edge properties, we iterate original edges
                    String edgeCondition = null;
                    for (CreateWorkflowRequest.Edge edge : edges) {
                        if (edge.getSource().equals(nodeId) && edge.getTarget().equals(nextNodeId)) {
                            edgeCondition = edge.getCondition();
                            break;
                        }
                    }
                    
                    // If edge has a condition (branchId), it must match the active branch
                    // STRICT MODE: If activeBranchId is set (Condition Node), edge MUST have matching condition
                    if (edgeCondition == null || !edgeCondition.equals(activeBranchId)) {
                        shouldTraverse = false;
                    }
                }

                if (shouldTraverse) {
                    inDegree.put(nextNodeId, inDegree.get(nextNodeId) - 1);
                    if (inDegree.get(nextNodeId) == 0) {
                        queue.offer(nextNodeId);
                    }
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
                    // Forward all input parameters (from context) to Start Node output
                    // This allows accessing inputs via {node_startId.param}
                    // This allows accessing inputs via {node_startId.param}
                    if (context != null) {
                        result.putAll(context);
                    }
                    log.info(">>> 起始节点执行完成");
                    break;
                case "end":
                    result.put("output", "工作流结束");
                    log.info(">>> 结束节点执行完成");
                    break;
                case "agent":
                case "llm":
                    // Get LLM client instance (Prototype bean)
                    DeepSeekClient llmClient = deepSeekClientProvider.getObject();
                    
                    // Prepare model configuration
                    Map<String, Object> modelConfig = new HashMap<>();
                    if (node.getData() != null) {
                        modelConfig.putAll(node.getData());
                    }
                    
                    // Replace variables in config (e.g. prompt)
                    String systemPrompt = (String) modelConfig.get("system_prompt");
                    if (systemPrompt != null) {
                        modelConfig.put("system_prompt", replaceVariables(systemPrompt, context));
                    }
                    
                    String userPrompt = (String) modelConfig.get("user_prompt");
                    if (userPrompt != null) {
                         // It might be stored as 'prompt' or 'user_prompt'
                        userPrompt = replaceVariables(userPrompt, context);
                    } else if (modelConfig.get("prompt") instanceof String) {
                        userPrompt = replaceVariables((String) modelConfig.get("prompt"), context);
                    }
                    
                    if (StringUtils.isEmpty(userPrompt)) {
                        // throw new IllegalArgumentException("User prompt is required for LLM node");
                        // Allow empty user prompt if system prompt is present, or just warn
                        log.warn("User prompt is empty for LLM node {}", node.getId());
                        userPrompt = " "; // Use empty space to avoid error
                    }
                    
                    // Call LLM
                    // If system prompt exists, append it as first message
                    if (StringUtils.hasText(systemPrompt)) {
                        llmClient.appendMessage("system", systemPrompt);
                    }
                    
                    // Handle output
                    DeepSeekClient.Message responseMessage = llmClient.chat(
                        null, // context (optional, maybe from input?)
                        userPrompt,
                        null, // tools (todo: support tools from config)
                        modelConfig
                    );
                    
                    if (responseMessage != null) {
                        result.put("output", responseMessage.getContent());
                        result.put("content", responseMessage.getContent()); // Add 'content' alias
                        result.put("message", responseMessage);
                        // Store structured result for next nodes
                        Map<String, Object> outputMap = new HashMap<>();
                        outputMap.put("response", responseMessage.getContent());
                        outputMap.put("role", responseMessage.getRole());
                        result.put("data", outputMap); 
                    } else {
                        result.put("output", "");
                        result.put("content", "");
                    }
                    
                    log.info(">>> LLM node executed successfully");
                    break;
                case "reply":
                    String content = (String) node.getData().get("content");
                    if (content != null) {
                        String processedContent = replaceVariables(content, context);
                        result.put("content", processedContent);
                        result.put("output", processedContent);
                        log.info(">>> 回复节点执行完成: {}", processedContent);
                    } else {
                        result.put("output", "");
                    }
                    break;
                case "condition":
                    // Evaluate conditions to find active branch
                    String activeBranchId = "branch_else"; // Default to ELSE
                    String activeBranchName = "ELSE";
                    
                    if (node.getData() != null && node.getData().containsKey("branches")) {
                        List<Map<String, Object>> branches = (List<Map<String, Object>>) node.getData().get("branches");
                        
                        for (Map<String, Object> branch : branches) {
                            String branchName = (String) branch.get("name");
                            if ("ELSE".equals(branchName)) continue;
                            
                            String logic = (String) branch.get("logic"); // AND / OR
                            List<Map<String, Object>> conditions = (List<Map<String, Object>>) branch.get("conditions");
                            
                            boolean branchMatched = false;
                            if (conditions == null || conditions.isEmpty()) {
                                branchMatched = true; // No conditions = true? Or false? Usually true for empty groups if logic is AND
                            } else {
                                if ("OR".equalsIgnoreCase(logic)) {
                                    branchMatched = false;
                                    for (Map<String, Object> cond : conditions) {
                                        if (evaluateCondition(cond, context)) {
                                            branchMatched = true;
                                            break;
                                        }
                                    }
                                } else { // AND
                                    branchMatched = true;
                                    for (Map<String, Object> cond : conditions) {
                                        if (!evaluateCondition(cond, context)) {
                                            branchMatched = false;
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            if (branchMatched) {
                                activeBranchId = (String) branch.get("id");
                                activeBranchName = branchName;
                                break; // Stop at first match (IF/ELIF logic)
                            }
                        }
                        
                        // If no match found, activeBranchId remains "branch_else" (or whatever the ELSE branch ID is)
                        if (activeBranchName.equals("ELSE")) {
                             // Find ELSE branch ID
                             for (Map<String, Object> branch : branches) {
                                 if ("ELSE".equals(branch.get("name"))) {
                                     activeBranchId = (String) branch.get("id");
                                     break;
                                 }
                             }
                        }
                    }
                    
                    result.put("output", "条件分支: " + activeBranchName);
                    result.put("activeBranchId", activeBranchId);
                    log.info(">>> 条件节点执行完成, 激活分支: {} ({})", activeBranchName, activeBranchId);
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

    private String replaceVariables(String text, Map<String, Object> context) {
        if (text == null) {
            return null;
        }

        // Match variable: {{node_id}} or {{node_id.field}} or {{input.param}}
        // Updated to use double curly braces {{...}} to avoid conflict with JSON
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(text);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varPath = matcher.group(1).trim(); // trim whitespace
            String replacement = matcher.group(0); // Default to original text if not found

            try {
                // Handle {input.param} format
                if (varPath.startsWith("input.")) {
                    String paramName = varPath.substring(6); // Remove "input."
                    Object inputObj = context.get("input"); // Assuming input is stored as "input" key or merged?
                    // Based on executeWorkflowDefinition, context starts with inputParams.
                    // But usually input params are directly in context or in a specific key?
                    // In executeWorkflowDefinition: Map<String, Object> context = new HashMap<>(inputParams != null ? inputParams : new HashMap<>());
                    // So input params are top-level keys in context.
                    // Wait, CodeHubot uses {input.param}, assuming there is an "input" dict.
                    // But here context IS the accumulated variables.
                    // Let's check if "input" node exists or if we should look up directly.
                    // If the user uses {input.foo}, they probably expect 'foo' from input.
                    // Since context was initialized with inputParams, we can check context.get(paramName).
                    
                    // However, to be strict compatible with CodeHubot's {input.param}, we might need to support both.
                    // If varPath is "input.param", we look for "param" in context.
                    Object value = context.get(paramName);
                    if (value != null) {
                        replacement = String.valueOf(value);
                    }
                } else {
                    // Handle {node_id} or {node_id.field} format
                    String[] parts = varPath.split("\\.", 2);
                    String nodeId = parts[0];
                    
                    if (context.containsKey(nodeId)) {
                        Object nodeOutput = context.get(nodeId);
                        
                        // If {node_id.field} format
                        if (parts.length > 1) {
                            String fieldPath = parts[1];
                            Map<String, Object> currentMap = (nodeOutput instanceof Map) ? (Map<String, Object>) nodeOutput : null;
                            
                            // Support nested field access, e.g., node_id.data.result
                            Object value = currentMap;
                            for (String field : fieldPath.split("\\.")) {
                                if (value instanceof Map) {
                                    value = ((Map<?, ?>) value).get(field);
                                } else {
                                    value = null; // Cannot access field on non-map
                                    break;
                                }
                            }
                            
                            if (value != null) {
                                replacement = String.valueOf(value);
                            }
                        } else {
                            // {node_id} format
                            if (nodeOutput instanceof String) {
                                replacement = (String) nodeOutput;
                            } else if (nodeOutput != null) {
                                try {
                                    replacement = objectMapper.writeValueAsString(nodeOutput);
                                } catch (Exception e) {
                                    replacement = nodeOutput.toString();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Variable replacement failed for {}: {}", varPath, e.getMessage());
            }
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
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

    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> context) {
        String variable = (String) condition.get("variable");
        String operator = (String) condition.get("operator");
        String value = (String) condition.get("value");
        
        // Resolve variable value from context
        // Variable format: {{node_id.field}} or {{input.param}} or just param name
        // We reuse replaceVariables logic but need the raw object, not string
        Object varValue = resolveVariableValue(variable, context);
        String varStr = varValue != null ? String.valueOf(varValue) : "";
        
        // Resolve target value (it might also contain variables)
        String targetVal = replaceVariables(value, context);
        if (targetVal == null) targetVal = "";
        
        switch (operator) {
            case "==": return varStr.equals(targetVal);
            case "!=": return !varStr.equals(targetVal);
            case ">": return compare(varStr, targetVal) > 0;
            case "<": return compare(varStr, targetVal) < 0;
            case ">=": return compare(varStr, targetVal) >= 0;
            case "<=": return compare(varStr, targetVal) <= 0;
            case "contains": return varStr.contains(targetVal);
            case "not_contains": return !varStr.contains(targetVal);
            case "start_with": return varStr.startsWith(targetVal);
            case "end_with": return varStr.endsWith(targetVal);
            case "is": return varStr.equals(targetVal);
            case "is_not": return !varStr.equals(targetVal);
            case "is_empty": return StringUtils.isEmpty(varStr);
            case "is_not_empty": return !StringUtils.isEmpty(varStr);
            default: return false;
        }
    }
    
    private int compare(String v1, String v2) {
        try {
            double d1 = Double.parseDouble(v1);
            double d2 = Double.parseDouble(v2);
            return Double.compare(d1, d2);
        } catch (NumberFormatException e) {
            return v1.compareTo(v2);
        }
    }
    
    private Object resolveVariableValue(String variable, Map<String, Object> context) {
        if (variable == null) return null;
        
        // Strip {{ }} if present
        if (variable.startsWith("{{") && variable.endsWith("}}")) {
            variable = variable.substring(2, variable.length() - 2).trim();
        }
        
        if (variable.startsWith("input.")) {
            return context.get(variable.substring(6));
        } else if (variable.contains(".")) {
            String[] parts = variable.split("\\.", 2);
            String nodeId = parts[0];
            String field = parts[1];
            
            Object nodeOutput = context.get(nodeId);
            if (nodeOutput instanceof Map) {
                return getNestedValue((Map<String, Object>) nodeOutput, field);
            }
        } else {
            return context.get(variable);
        }
        return null;
    }
    
    private Object getNestedValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}