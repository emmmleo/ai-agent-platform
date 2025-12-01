package com.aiagent.agent.service.impl;

import com.aiagent.agent.client.DeepSeekClient;
import com.aiagent.agent.dto.AgentResponse;
import com.aiagent.agent.dto.ChatRequest;
import com.aiagent.agent.dto.ChatResponse;
import com.aiagent.agent.dto.CreateAgentRequest;
import com.aiagent.agent.dto.TestAgentRequest;
import com.aiagent.agent.dto.TestAgentResponse;
import com.aiagent.agent.dto.UpdateAgentRequest;
import com.aiagent.agent.entity.Agent;
import com.aiagent.agent.mapper.AgentMapper;
import com.aiagent.agent.service.AgentService;
import com.aiagent.plugin.client.PluginInvocationClient;
import com.aiagent.plugin.client.PluginInvocationResult;
import com.aiagent.plugin.entity.Plugin;
import com.aiagent.plugin.mapper.PluginMapper;
import com.aiagent.plugin.tooling.OpenApiToolingBuilder;
import com.aiagent.plugin.tooling.PluginOperationDescriptor;
import com.aiagent.plugin.tooling.PluginTooling;
import com.aiagent.workflow.service.WorkflowExecutionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private static final String DEFAULT_STATUS = "draft";
    private static final int MAX_TOOL_CALL_LOOPS = 5;

    private final AgentMapper agentMapper;
    private final WorkflowExecutionService workflowExecutionService;
    private final DeepSeekClient deepSeekClient;
    private final PluginMapper pluginMapper;
    private final OpenApiToolingBuilder openApiToolingBuilder;
    private final PluginInvocationClient pluginInvocationClient;
    private final ObjectMapper objectMapper;

    public AgentServiceImpl(AgentMapper agentMapper,
                           WorkflowExecutionService workflowExecutionService,
                           DeepSeekClient deepSeekClient,
                           PluginMapper pluginMapper,
                           OpenApiToolingBuilder openApiToolingBuilder,
                           PluginInvocationClient pluginInvocationClient,
                           ObjectMapper objectMapper) {
        this.agentMapper = agentMapper;
        this.workflowExecutionService = workflowExecutionService;
        this.deepSeekClient = deepSeekClient;
        this.pluginMapper = pluginMapper;
        this.openApiToolingBuilder = openApiToolingBuilder;
        this.pluginInvocationClient = pluginInvocationClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AgentResponse createAgent(Long userId, CreateAgentRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Agent agent = new Agent();
        agent.setUserId(userId);
        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setSystemPrompt(request.getSystemPrompt());
        agent.setUserPromptTemplate(request.getUserPromptTemplate());
        agent.setModelConfig(request.getModelConfig());
        agent.setWorkflowId(request.getWorkflowId());
        agent.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        agent.setPluginIds(request.getPluginIds());
        agent.setStatus(DEFAULT_STATUS);
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);

        agentMapper.insert(agent);

        return toResponse(agent);
    }

    @Override
    public List<AgentResponse> getAgentsByUserId(Long userId) {
        try {
            List<Agent> agents = agentMapper.findByUserId(userId);
            return agents.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching agents for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public AgentResponse getAgentById(Long id, Long userId) {
        Agent agent = agentMapper.findByIdAndUserId(id, userId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或无权限访问");
        }
        return toResponse(agent);
    }

    @Override
    @Transactional
    public AgentResponse updateAgent(Long id, Long userId, UpdateAgentRequest request) {
        Agent agent = agentMapper.findByIdAndUserId(id, userId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或无权限访问");
        }

        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setSystemPrompt(request.getSystemPrompt());
        agent.setUserPromptTemplate(request.getUserPromptTemplate());
        agent.setModelConfig(request.getModelConfig());
        agent.setWorkflowId(request.getWorkflowId());
        agent.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        agent.setPluginIds(request.getPluginIds());
        if (request.getStatus() != null) {
            agent.setStatus(request.getStatus());
        }
        agent.setUpdatedAt(LocalDateTime.now());

        agentMapper.update(agent);

        return toResponse(agent);
    }

    @Override
    @Transactional
    public void deleteAgent(Long id, Long userId) {
        Agent agent = agentMapper.findByIdAndUserId(id, userId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或无权限访问");
        }
        agentMapper.deleteById(id);
    }

    @Override
    public TestAgentResponse testAgent(Long id, Long userId, TestAgentRequest request) {
        Agent agent = agentMapper.findByIdAndUserId(id, userId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或无权限访问");
        }

        try {
            ModelAnswer modelAnswer = callAIModel(agent, request.getQuestion(), "");
            return new TestAgentResponse(modelAnswer.content(), modelAnswer.pluginsUsed());
        } catch (Exception e) {
            log.error("测试智能体失败", e);
            throw new RuntimeException("测试智能体失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public AgentResponse publishAgent(Long id, Long userId) {
        Agent agent = agentMapper.findByIdAndUserId(id, userId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或无权限访问");
        }

        // 验证配置完整性
        List<String> missingFields = new ArrayList<>();
        if (agent.getSystemPrompt() == null || agent.getSystemPrompt().trim().isEmpty()) {
            missingFields.add("系统提示词");
        }
        if (agent.getName() == null || agent.getName().trim().isEmpty()) {
            missingFields.add("智能体名称");
        }

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("智能体配置不完整，缺少以下配置项：" + String.join("、", missingFields));
        }

        // 更新状态为已发布
        agent.setStatus("published");
        agent.setUpdatedAt(LocalDateTime.now());
        agentMapper.update(agent);

        return toResponse(agent);
    }

    @Override
    public ChatResponse chat(Long id, Long userId, ChatRequest request) {
        Agent agent = agentMapper.findById(id);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在");
        }

        // 检查智能体是否已发布
        if (!"published".equals(agent.getStatus())) {
            throw new IllegalArgumentException("智能体未发布，无法使用");
        }

        // 检查权限（智能体所有者或公开访问）
        if (!agent.getUserId().equals(userId)) {
            // 可以在这里添加公开访问的逻辑
            throw new IllegalArgumentException("无权限访问此智能体");
        }

        String answer;
        String source = "direct";
        List<String> pluginsUsed = Collections.emptyList();

        try {
            // 1. 如果配置了知识库，进行RAG检索
            String context = "";
            if (agent.getKnowledgeBaseIds() != null && !agent.getKnowledgeBaseIds().trim().isEmpty()) {
                try {
                    List<Long> knowledgeBaseIds = parseJsonArray(agent.getKnowledgeBaseIds());
                    context = retrieveFromKnowledgeBases(knowledgeBaseIds, request.getQuestion());
                    if (!context.isEmpty()) {
                        source = "rag";
                    }
                } catch (Exception e) {
                    log.warn("RAG检索失败: {}", e.getMessage());
                }
            }

            // 2. 如果配置了工作流，执行工作流
            if (agent.getWorkflowId() != null) {
                try {
                    String workflowResult = executeWorkflow(agent.getWorkflowId(), userId, request.getQuestion());
                    if (workflowResult != null && !workflowResult.isEmpty()) {
                        answer = workflowResult;
                        source = "workflow";
                        pluginsUsed = Collections.emptyList();
                    } else {
                        // 工作流执行失败，回退到直接调用AI
                        ModelAnswer modelAnswer = callAIModel(agent, request.getQuestion(), context);
                        answer = modelAnswer.content();
                        pluginsUsed = modelAnswer.pluginsUsed();
                    }
                } catch (Exception e) {
                    log.warn("工作流执行失败: {}", e.getMessage());
                    // 工作流执行失败，回退到直接调用AI
                    ModelAnswer modelAnswer = callAIModel(agent, request.getQuestion(), context);
                    answer = modelAnswer.content();
                    pluginsUsed = modelAnswer.pluginsUsed();
                }
            } else {
                // 3. 直接调用AI模型
                ModelAnswer modelAnswer = callAIModel(agent, request.getQuestion(), context);
                answer = modelAnswer.content();
                pluginsUsed = modelAnswer.pluginsUsed();
            }

            return new ChatResponse(answer, source, pluginsUsed);
        } catch (Exception e) {
            log.error("智能体对话失败", e);
            throw new RuntimeException("智能体对话失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从知识库检索相关内容（RAG）
     */
    private String retrieveFromKnowledgeBases(List<Long> knowledgeBaseIds, String question) {
        // TODO: 实现向量检索逻辑
        // 1. 将问题向量化
        // 2. 在知识库中搜索相似文档块
        // 3. 返回相关上下文
        
        // 目前返回模拟结果
        log.info("RAG检索：知识库IDs: {}, 问题: {}", knowledgeBaseIds, question);
        return "相关上下文：这是从知识库检索到的相关内容（模拟）。实际应用中，这里会进行向量相似度搜索。";
    }

    /**
     * 执行工作流
     */
    private String executeWorkflow(Long workflowId, Long userId, String question) {
        try {
            // 创建工作流执行请求
            com.aiagent.workflow.dto.ExecuteWorkflowRequest request = new com.aiagent.workflow.dto.ExecuteWorkflowRequest();
            Map<String, Object> inputParams = new java.util.HashMap<>();
            inputParams.put("question", question);
            request.setInputParams(inputParams);

            // 执行工作流
            com.aiagent.workflow.dto.WorkflowExecutionResponse execution = 
                workflowExecutionService.executeWorkflow(workflowId, userId, request);

            // 等待执行完成（简化处理，实际应该异步等待）
            // 这里返回执行ID，实际应该等待执行完成并获取结果
            return "工作流执行中（执行ID: " + execution.getId() + "）。实际应用中，这里应该等待工作流执行完成并返回结果。";
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            throw e;
        }
    }

    /**
     * 调用AI模型
     */
    private ModelAnswer callAIModel(Agent agent, String question, String context) {
        String systemPrompt = agent.getSystemPrompt() != null ? agent.getSystemPrompt() : "";
        Map<String, Object> modelConfig = parseModelConfig(agent.getModelConfig());

        log.info("调用AI模型：系统提示词长度: {}, 模型配置字段: {}, 问题: {}",
                systemPrompt.length(), modelConfig.keySet(), question);

        PluginTooling pluginTooling = resolvePluginTooling(agent);
        log.info("解析插件成功");
        if (pluginTooling == null || pluginTooling.getTools().isEmpty()) {
            String response = deepSeekClient.chat(systemPrompt, context, question, modelConfig);
            return new ModelAnswer(response, Collections.emptyList());
        }
        return chatWithFunctionCalling(systemPrompt, context, question, modelConfig, pluginTooling);
    }

    private PluginTooling resolvePluginTooling(Agent agent) {
        if (agent.getPluginIds() == null || agent.getPluginIds().trim().isEmpty()) {
            return null;
        }
        List<Long> pluginIds = parseJsonArray(agent.getPluginIds());
        if (pluginIds.isEmpty()) {
            return null;
        }
        List<Plugin> plugins = pluginMapper.findByIds(pluginIds);
        if (plugins == null || plugins.isEmpty()) {
            return null;
        }
        Map<Long, Plugin> pluginById = plugins.stream()
            .filter(p -> p.getId() != null)
            .collect(Collectors.toMap(Plugin::getId, p -> p, (left, right) -> left));
        List<Plugin> ordered = pluginIds.stream()
            .map(pluginById::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return openApiToolingBuilder.build(ordered);
    }

    private ModelAnswer chatWithFunctionCalling(String systemPrompt,
                                                String context,
                                                String question,
                                                Map<String, Object> modelConfig,
                                                PluginTooling pluginTooling) {
        List<DeepSeekClient.Message> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(new DeepSeekClient.Message("system", systemPrompt));
        }
        StringBuilder userContent = new StringBuilder();
        if (StringUtils.hasText(context)) {
            userContent.append("相关上下文：").append(context).append("\n\n");
        }
        userContent.append(question);
        messages.add(new DeepSeekClient.Message("user", userContent.toString()));
        Set<String> invokedPlugins = new LinkedHashSet<>();

        for (int round = 0; round < MAX_TOOL_CALL_LOOPS; round++) {
            DeepSeekClient.ChatCompletionResponse response = deepSeekClient.chat(messages, pluginTooling.getTools(), modelConfig);
            DeepSeekClient.Message assistantMessage = extractAssistantMessage(response);
            if (assistantMessage == null) {
                throw new IllegalStateException("LLM响应为空");
            }
            messages.add(assistantMessage);

            List<DeepSeekClient.ToolCall> toolCalls = assistantMessage.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                String content = assistantMessage.getContent();
                if (!StringUtils.hasText(content)) {
                    throw new IllegalStateException("LLM响应缺少内容");
                }
                return new ModelAnswer(content, List.copyOf(invokedPlugins));
            }
            for (DeepSeekClient.ToolCall toolCall : toolCalls) {
                log.info("执行 \"" + toolCall.getFunction().getName() + "\"");
                ToolCallResult toolResponse = executeToolCall(toolCall, pluginTooling.getOperationRegistry());
                messages.add(new DeepSeekClient.Message("tool", toolResponse.payload(), toolCall.getId()));
                if (StringUtils.hasText(toolResponse.pluginName())) {
                    invokedPlugins.add(toolResponse.pluginName());
                }
            }
        }
        throw new IllegalStateException("函数调用轮次数超限");
    }

    private DeepSeekClient.Message extractAssistantMessage(DeepSeekClient.ChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return null;
        }
        DeepSeekClient.Choice first = response.getChoices().get(0);
        return first != null ? first.getMessage() : null;
    }

    private ToolCallResult executeToolCall(DeepSeekClient.ToolCall toolCall,
                                           Map<String, PluginOperationDescriptor> registry) {
        if (toolCall == null || toolCall.getFunction() == null) {
            return new ToolCallResult(buildToolErrorPayload(null, "无效的tool call"), null);
        }
        String functionName = toolCall.getFunction().getName();
        PluginOperationDescriptor descriptor = registry.get(functionName);
        if (descriptor == null) {
            log.warn("未找到函数{}的插件描述，无法执行", functionName);
            return new ToolCallResult(buildToolErrorPayload(null, "未找到插件定义"), null);
        }
        JsonNode arguments = parseArguments(toolCall.getFunction().getArguments());
        log.info("解析参数 \"" + functionName + "\"");
        PluginInvocationResult result = pluginInvocationClient.invoke(descriptor, arguments);
        log.info("返回结果 \"" + functionName + "\"");
        String payload = buildToolResultPayload(descriptor, result);
        return new ToolCallResult(payload, descriptor.getPluginName());
    }

    private JsonNode parseArguments(String argumentsJson) {
        if (!StringUtils.hasText(argumentsJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(argumentsJson);
        } catch (Exception e) {
            log.warn("解析函数参数失败: {}", argumentsJson, e);
            return objectMapper.createObjectNode();
        }
    }

    private String buildToolResultPayload(PluginOperationDescriptor descriptor, PluginInvocationResult result) {
        ObjectNode payload = objectMapper.createObjectNode();
        if (descriptor != null) {
            payload.put("pluginId", descriptor.getPluginId());
            payload.put("pluginName", descriptor.getPluginName());
            payload.put("endpoint", descriptor.getPath());
        }
        if (result == null) {
            payload.put("success", false);
            payload.put("errorMessage", "插件执行返回为空");
            return payload.toString();
        }
        payload.put("success", result.isSuccess());
        payload.put("statusCode", result.getStatusCode());
        if (result.getBody() != null) {
            JsonNode bodyNode = tryParseJson(result.getBody());
            if (bodyNode != null) {
                payload.set(result.isSuccess() ? "data" : "errorBody", bodyNode);
            } else if (result.isSuccess()) {
                payload.put("data", result.getBody());
            } else {
                payload.put("errorBody", result.getBody());
            }
        }
        if (!result.isSuccess() && result.getErrorMessage() != null) {
            payload.put("errorMessage", result.getErrorMessage());
        }
        return payload.toString();
    }

    private String buildToolErrorPayload(PluginOperationDescriptor descriptor, String message) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("success", false);
        if (descriptor != null) {
            payload.put("pluginId", descriptor.getPluginId());
            payload.put("pluginName", descriptor.getPluginName());
        }
        payload.put("errorMessage", message);
        return payload.toString();
    }

    private JsonNode tryParseJson(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            return objectMapper.readTree(content);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 解析JSON数组字符串
     */
    private List<Long> parseJsonArray(String jsonArray) {
        try {
            List<?> list = objectMapper.readValue(jsonArray, List.class);
            return list.stream()
                    .map(item -> {
                        if (item instanceof Number) {
                            return ((Number) item).longValue();
                        }
                        return Long.parseLong(item.toString());
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("解析JSON数组失败: {}", jsonArray, e);
            return new ArrayList<>();
        }
    }

    private Map<String, Object> parseModelConfig(String modelConfigJson) {
        if (modelConfigJson == null || modelConfigJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(modelConfigJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析modelConfig失败: {}", modelConfigJson, e);
            return Collections.emptyMap();
        }
    }

    private AgentResponse toResponse(Agent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getUserId(),
                agent.getName(),
                agent.getDescription(),
                agent.getSystemPrompt(),
                agent.getUserPromptTemplate(),
                agent.getModelConfig(),
                agent.getWorkflowId(),
                agent.getKnowledgeBaseIds(),
                agent.getPluginIds(),
                agent.getStatus(),
                agent.getCreatedAt(),
                agent.getUpdatedAt()
        );
    }

        private String generateMockAnswer(String systemPrompt, String question) {
        // 方法保留以兼容旧流程，但不再在生产路径中调用
        String promptPreview = systemPrompt != null && systemPrompt.length() > 50 
            ? systemPrompt.substring(0, 50) + "..." 
            : (systemPrompt != null ? systemPrompt : "无");
        return String.format("基于系统提示词：%s\n\n问题：%s\n\n回答：这是一个模拟回答。实际应用中，这里会调用AI模型（如OpenAI、Claude等）来生成回答。", 
            promptPreview, question);
        }

    private record ModelAnswer(String content, List<String> pluginsUsed) {
        private ModelAnswer {
            pluginsUsed = pluginsUsed == null ? Collections.emptyList() : List.copyOf(pluginsUsed);
        }
    }

    private record ToolCallResult(String payload, String pluginName) {}
}
