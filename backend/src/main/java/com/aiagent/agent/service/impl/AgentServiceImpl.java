package com.aiagent.agent.service.impl;

import com.aiagent.agent.client.DeepSeekClient;
import com.aiagent.agent.dto.AgentResponse;
import com.aiagent.agent.dto.ChatHistoryResponse;
import com.aiagent.agent.dto.ChatRequest;
import com.aiagent.agent.dto.ChatResponse;
import com.aiagent.agent.dto.ChatSessionResponse;
import com.aiagent.agent.dto.ChatSessionsResponse;
import com.aiagent.agent.dto.CreateAgentRequest;
import com.aiagent.agent.dto.TestAgentRequest;
import com.aiagent.agent.dto.TestAgentResponse;
import com.aiagent.agent.dto.UpdateAgentRequest;
import com.aiagent.agent.entity.Agent;
import com.aiagent.agent.entity.AgentConversationContext;
import com.aiagent.agent.mapper.AgentConversationContextMapper;
import com.aiagent.agent.mapper.AgentMapper;
import com.aiagent.agent.service.AgentService;
import com.aiagent.knowledgebase.util.VectorStoreService;
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
import org.springframework.beans.factory.ObjectProvider;
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
    private final AgentConversationContextMapper conversationContextMapper;
    private final ObjectProvider<DeepSeekClient> deepSeekClientProvider;
    private final PluginMapper pluginMapper;
    private final OpenApiToolingBuilder openApiToolingBuilder;
    private final PluginInvocationClient pluginInvocationClient;
    private final ObjectMapper objectMapper;
    private final VectorStoreService vectorStoreService;

    public AgentServiceImpl(AgentMapper agentMapper,
                           WorkflowExecutionService workflowExecutionService,
                           AgentConversationContextMapper conversationContextMapper,
                           ObjectProvider<DeepSeekClient> deepSeekClientProvider,
                           PluginMapper pluginMapper,
                           OpenApiToolingBuilder openApiToolingBuilder,
                           PluginInvocationClient pluginInvocationClient,
                           ObjectMapper objectMapper,
                           VectorStoreService vectorStoreService) {
        this.agentMapper = agentMapper;
        this.workflowExecutionService = workflowExecutionService;
        this.conversationContextMapper = conversationContextMapper;
        this.deepSeekClientProvider = deepSeekClientProvider;
        this.pluginMapper = pluginMapper;
        this.openApiToolingBuilder = openApiToolingBuilder;
        this.pluginInvocationClient = pluginInvocationClient;
        this.objectMapper = objectMapper;
        this.vectorStoreService = vectorStoreService;
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
            ModelAnswer modelAnswer = callAIModel(agent, userId, null, request.getQuestion(), "", false, null);
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
        Long sessionId = request.getSessionId();
        ChatResponse.RagContext ragContext = null;

        try {
            // 1. 如果配置了知识库，进行RAG检索
            String context = "";
            if (agent.getKnowledgeBaseIds() != null && !agent.getKnowledgeBaseIds().trim().isEmpty()) {
                try {
                    List<Long> knowledgeBaseIds = parseJsonArray(agent.getKnowledgeBaseIds());
                    RetrievalResult retrievalResult = retrieveFromKnowledgeBases(knowledgeBaseIds, request.getQuestion());
                    context = retrievalResult.context();
                    ragContext = retrievalResult.ragContext();
                    
                    if (StringUtils.hasText(context)) {
                        source = "rag";
                    }
                    
                    log.info("RAG检索完成: context长度={}, ragContext.references={}", 
                        context.length(), 
                        ragContext != null && ragContext.getReferences() != null ? ragContext.getReferences().size() : 0);
                } catch (Exception e) {
                    log.warn("RAG检索失败: {}", e.getMessage());
                    ragContext = new ChatResponse.RagContext(false, "检索异常: " + e.getMessage(), Collections.emptyList());
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
                        ModelAnswer modelAnswer = callAIModel(agent, userId, sessionId, request.getQuestion(), context, true, ragContext);
                        answer = modelAnswer.content();
                        pluginsUsed = modelAnswer.pluginsUsed();
                        sessionId = modelAnswer.sessionId();
                        ragContext = modelAnswer.ragContext();
                    }
                } catch (Exception e) {
                    log.warn("工作流执行失败: {}", e.getMessage());
                    // 工作流执行失败，回退到直接调用AI
                    ModelAnswer modelAnswer = callAIModel(agent, userId, sessionId, request.getQuestion(), context, true, ragContext);
                    answer = modelAnswer.content();
                    pluginsUsed = modelAnswer.pluginsUsed();
                    sessionId = modelAnswer.sessionId();
                    ragContext = modelAnswer.ragContext();
                }
            } else {
                // 3. 直接调用AI模型
                ModelAnswer modelAnswer = callAIModel(agent, userId, sessionId, request.getQuestion(), context, true, ragContext);
                answer = modelAnswer.content();
                pluginsUsed = modelAnswer.pluginsUsed();
                sessionId = modelAnswer.sessionId();
                ragContext = modelAnswer.ragContext();
            }

            ChatResponse chatResponse = new ChatResponse(answer, source, pluginsUsed, sessionId, ragContext);
            
            log.info("准备返回响应: source={}, pluginsUsed={}, ragContext.references={}", 
                source, 
                pluginsUsed != null ? pluginsUsed.size() : 0,
                ragContext != null && ragContext.getReferences() != null ? ragContext.getReferences().size() : 0);
            
            // 打印完整的响应对象用于调试
            try {
                String jsonResponse = objectMapper.writeValueAsString(chatResponse);
                log.info("完整响应JSON: {}", jsonResponse);
            } catch (Exception e) {
                log.warn("无法序列化响应为JSON", e);
            }

            return chatResponse;
        } catch (Exception e) {
            log.error("智能体对话失败", e);
            throw new RuntimeException("智能体对话失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatHistoryResponse getConversation(Long id, Long userId, Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        Agent agent = agentMapper.findByIdAndUserId(id, userId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或无权限访问");
        }
        AgentConversationContext context = conversationContextMapper.findByIdAndOwner(sessionId, id, userId);
        if (context == null) {
            throw new IllegalArgumentException("会话不存在或无权限访问");
        }
        List<DeepSeekClient.Message> history = parseConversationMessages(context.getMessages(), agent.getSystemPrompt());
        ChatHistoryResponse response = new ChatHistoryResponse();
        response.setSessionId(sessionId);
        response.setMessages(toChatHistoryMessages(history));
        return response;
    }

    @Override
    public ChatSessionsResponse listConversations(Long id, Long userId) {
        Agent agent = agentMapper.findByIdAndUserId(id, userId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或无权限访问");
        }
        List<AgentConversationContext> contexts = conversationContextMapper.findSessions(id, userId);
        List<ChatSessionResponse> sessions = contexts.stream()
            .map(ctx -> new ChatSessionResponse(ctx.getId(),
                StringUtils.hasText(ctx.getTitle()) ? ctx.getTitle() : buildDefaultTitle(ctx.getId()),
                ctx.getUpdatedAt()))
                .collect(Collectors.toList());
        ChatSessionsResponse response = new ChatSessionsResponse();
        response.setSessions(sessions);
        return response;
    }

    @Override
    @Transactional
    public ChatSessionResponse createConversation(Long id, Long userId) {
        Agent agent = agentMapper.findByIdAndUserId(id, userId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或无权限访问");
        }
        List<DeepSeekClient.Message> initial = initializeConversation(agent.getSystemPrompt());
        Long sessionId = persistConversationContext(agent.getId(), userId, initial, null);
        AgentConversationContext context = conversationContextMapper.findById(sessionId);
        if (context == null) {
            throw new IllegalStateException("创建会话失败");
        }
        String title = StringUtils.hasText(context.getTitle()) ? context.getTitle() : buildDefaultTitle(context.getId());
        return new ChatSessionResponse(context.getId(), title, context.getUpdatedAt());
    }

    @Override
    @Transactional
    public void deleteConversation(Long id, Long userId, Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        Agent agent = agentMapper.findByIdAndUserId(id, userId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在或无权限访问");
        }
        AgentConversationContext context = conversationContextMapper.findByIdAndOwner(sessionId, id, userId);
        if (context == null) {
            throw new IllegalArgumentException("会话不存在或无权限访问");
        }
        conversationContextMapper.deleteById(sessionId);
    }

    /**
     * 从知识库检索相关内容（RAG）
     */
    private RetrievalResult retrieveFromKnowledgeBases(List<Long> knowledgeBaseIds, String question) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return new RetrievalResult("", null);
        }

        try {
            log.info("开始执行RAG检索: knowledgeBaseIds={}, question={}", knowledgeBaseIds, question);
            
            // 先进行诊断检查（仅在DEBUG级别时输出完整信息）
            if (log.isDebugEnabled()) {
                String diagnoseInfo = vectorStoreService.diagnose(knowledgeBaseIds);
                log.debug("向量存储诊断:\n{}", diagnoseInfo);
            }
            
            // 调用向量服务进行检索，默认取Top 1
            List<VectorStoreService.ChunkSearchResult> results = vectorStoreService.search(knowledgeBaseIds, question, null);
            
            if (results.isEmpty()) {
                log.warn("RAG检索未找到匹配内容，执行诊断检查...");
                String diagnoseInfo = vectorStoreService.diagnose(knowledgeBaseIds);
                log.warn("诊断结果:\n{}", diagnoseInfo);
                
                ChatResponse.RagContext ragContext = new ChatResponse.RagContext(true, "未找到相关内容", Collections.emptyList());
                return new RetrievalResult("", ragContext);
            }

            log.info("RAG检索成功，命中 {} 个片段", results.size());

            // 格式化检索结果
            StringBuilder contextBuilder = new StringBuilder();
            List<ChatResponse.RagReference> references = new ArrayList<>();
            contextBuilder.append("基于以下知识库片段回答问题：\n\n");
            
            for (int i = 0; i < results.size(); i++) {
                VectorStoreService.ChunkSearchResult result = results.get(i);
                
                log.debug("片段 #{}: kbId={}, docId={}, score={}, content_preview={}", 
                    i + 1, result.knowledgeBaseId(), result.documentId(), result.score(), 
                    result.content() != null && result.content().length() > 50 ? result.content().substring(0, 50) + "..." : result.content());

                // 构建上下文文本
                contextBuilder.append(String.format("[参考片段 %d] (相关度: %.4f)\n", i + 1, result.score()));
                contextBuilder.append(result.content());
                contextBuilder.append("\n\n");
                
                // 构建引用元数据
                references.add(new ChatResponse.RagReference(
                    result.knowledgeBaseId(),
                    result.documentId(),
                    result.content(),
                    result.score()
                ));
            }
            
            ChatResponse.RagContext ragContext = new ChatResponse.RagContext(true, "检索成功", references);
            return new RetrievalResult(contextBuilder.toString(), ragContext);
            
        } catch (Exception e) {
            log.error("RAG检索失败: knowledgeBaseIds={}, question={}", knowledgeBaseIds, question, e);
            // 检索失败不应阻断对话，返回空字符串降级处理，并返回错误信息
            ChatResponse.RagContext ragContext = new ChatResponse.RagContext(false, "检索失败: " + e.getMessage(), Collections.emptyList());
            return new RetrievalResult("", ragContext);
        }
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
    private ModelAnswer callAIModel(Agent agent,
                                   Long userId,
                                   Long sessionId,
                                   String question,
                                   String context,
                                   boolean persistContext,
                                   ChatResponse.RagContext ragContext) {
        String systemPrompt = agent.getSystemPrompt() != null ? agent.getSystemPrompt() : "";
        Map<String, Object> modelConfig = parseModelConfig(agent.getModelConfig());

        log.info("调用AI模型：系统提示词长度: {}, 模型配置字段: {}, 问题: {}",
                systemPrompt.length(), modelConfig.keySet(), question);

        ConversationState conversationState = prepareConversationState(agent, userId, systemPrompt, persistContext, sessionId);
        List<DeepSeekClient.Message> conversation = conversationState.messages();
        DeepSeekClient client = createClient(conversation);

        // 如果有知识库上下文，作为临时的系统消息添加（不持久化到历史中）
        int tempMessageStartIndex = conversation.size();
        if (StringUtils.hasText(context)) {
            DeepSeekClient.Message contextMessage = new DeepSeekClient.Message("system", context);
            conversation.add(contextMessage);
            client.appendMessage(contextMessage);
        }

        // 只添加用户的原始问题
        DeepSeekClient.Message userMessage = new DeepSeekClient.Message("user", question);
        conversation.add(userMessage);

        PluginTooling pluginTooling = resolvePluginTooling(agent);
        ModelAnswer modelAnswer;
        if (pluginTooling == null || pluginTooling.getTools().isEmpty()) {
            DeepSeekClient.Message assistantMessage = client.chat(context, question, null, modelConfig);
            if (assistantMessage == null || !StringUtils.hasText(assistantMessage.getContent())) {
                throw new IllegalStateException("LLM响应为空");
            }
            assistantMessage.setRagContext(ragContext);
            conversation.add(assistantMessage);
            modelAnswer = new ModelAnswer(assistantMessage.getContent(), Collections.emptyList(), null, ragContext);
        } else {
            modelAnswer = chatWithFunctionCalling(client, conversation, context, question, modelConfig, pluginTooling, ragContext);
        }

        if (persistContext) {
            // 移除临时的知识库上下文消息，只保存用户问题和助手回复
            if (StringUtils.hasText(context) && tempMessageStartIndex < conversation.size()) {
                conversation.remove(tempMessageStartIndex);
            }
            Long persistedSessionId = persistConversationContext(agent.getId(), userId, conversation, conversationState.existingContext());
            modelAnswer = new ModelAnswer(modelAnswer.content(), modelAnswer.pluginsUsed(), persistedSessionId, modelAnswer.ragContext());
        }
        return modelAnswer;
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

    private ModelAnswer chatWithFunctionCalling(DeepSeekClient client,
                                                List<DeepSeekClient.Message> conversation,
                                                String context,
                                                String question,
                                                Map<String, Object> modelConfig,
                                                PluginTooling pluginTooling,
                                                ChatResponse.RagContext ragContext) {
        Set<String> invokedPlugins = new LinkedHashSet<>();
        DeepSeekClient.Message assistantMessage = client.chat(context, question, pluginTooling.getTools(), modelConfig);
        if (assistantMessage == null) {
            throw new IllegalStateException("LLM响应为空");
        }
        conversation.add(assistantMessage);

        for (int round = 0; round < MAX_TOOL_CALL_LOOPS; round++) {
            List<DeepSeekClient.ToolCall> toolCalls = assistantMessage.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                String content = assistantMessage.getContent();
                if (!StringUtils.hasText(content)) {
                    throw new IllegalStateException("LLM响应缺少内容");
                }
                List<String> plugins = new ArrayList<>(invokedPlugins);
                if (!plugins.isEmpty()) {
                    assistantMessage.setPlugins(new ArrayList<>(plugins));
                }
                assistantMessage.setRagContext(ragContext);
                return new ModelAnswer(content, plugins, null, ragContext);
            }
            for (DeepSeekClient.ToolCall toolCall : toolCalls) {
                log.info("执行 \"{}\"", toolCall.getFunction().getName());
                ToolCallResult toolResponse = executeToolCall(toolCall, pluginTooling.getOperationRegistry());
                DeepSeekClient.Message toolMessage = new DeepSeekClient.Message("tool", toolResponse.payload(), toolCall.getId());
                client.appendMessage(toolMessage);
                conversation.add(toolMessage);
                if (StringUtils.hasText(toolResponse.pluginName())) {
                    invokedPlugins.add(toolResponse.pluginName());
                }
            }
            assistantMessage = client.continueChat(pluginTooling.getTools(), modelConfig);
            if (assistantMessage == null) {
                throw new IllegalStateException("LLM响应为空");
            }
            conversation.add(assistantMessage);
        }
        throw new IllegalStateException("函数调用轮次数超限");
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

    private ConversationState prepareConversationState(Agent agent,
                                                       Long userId,
                                                       String systemPrompt,
                                                       boolean persistContext,
                                                       Long sessionId) {
        if (!persistContext) {
            return new ConversationState(initializeConversation(systemPrompt), null);
        }
        if (sessionId != null) {
            AgentConversationContext existing = conversationContextMapper.findByIdAndOwner(sessionId, agent.getId(), userId);
            if (existing == null) {
                throw new IllegalArgumentException("会话不存在或无权限访问");
            }
            List<DeepSeekClient.Message> history = parseConversationMessages(existing.getMessages(), systemPrompt);
            return new ConversationState(history, existing);
        }
        return new ConversationState(initializeConversation(systemPrompt), null);
    }

    private List<DeepSeekClient.Message> initializeConversation(String systemPrompt) {
        List<DeepSeekClient.Message> history = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            history.add(new DeepSeekClient.Message("system", systemPrompt));
        }
        return history;
    }

    private List<DeepSeekClient.Message> parseConversationMessages(String payload, String systemPrompt) {
        if (!StringUtils.hasText(payload)) {
            return initializeConversation(systemPrompt);
        }
        try {
            List<DeepSeekClient.Message> history = objectMapper.readValue(
                    payload, new TypeReference<List<DeepSeekClient.Message>>() {});
            history = history == null ? new ArrayList<>() : new ArrayList<>(history);
            ensureSystemPrompt(history, systemPrompt);
            return history;
        } catch (Exception e) {
            log.warn("解析对话上下文失败，将重新初始化: {}", e.getMessage());
            return initializeConversation(systemPrompt);
        }
    }

    private void ensureSystemPrompt(List<DeepSeekClient.Message> history, String systemPrompt) {
        if (!StringUtils.hasText(systemPrompt)) {
            return;
        }
        if (history.isEmpty()) {
            history.add(new DeepSeekClient.Message("system", systemPrompt));
            return;
        }
        DeepSeekClient.Message first = history.get(0);
        if ("system".equals(first.getRole())) {
            first.setContent(systemPrompt);
        } else {
            history.add(0, new DeepSeekClient.Message("system", systemPrompt));
        }
    }

    private DeepSeekClient createClient(List<DeepSeekClient.Message> history) {
        DeepSeekClient client = deepSeekClientProvider.getObject();
        if (history != null) {
            history.forEach(client::appendMessage);
        }
        return client;
    }

    private Long persistConversationContext(Long agentId,
                                            Long userId,
                                            List<DeepSeekClient.Message> messages,
                                            AgentConversationContext existingContext) {
        String payload = serializeMessages(messages);
        LocalDateTime now = LocalDateTime.now();
        if (existingContext == null) {
            AgentConversationContext context = new AgentConversationContext();
            context.setAgentId(agentId);
            context.setUserId(userId);
            context.setTitle("会话");
            context.setMessages(payload);
            context.setCreatedAt(now);
            context.setUpdatedAt(now);
            conversationContextMapper.insert(context);
            String title = buildDefaultTitle(context.getId());
            conversationContextMapper.updateTitle(context.getId(), title, now);
            return context.getId();
        }
        conversationContextMapper.updateMessagesById(existingContext.getId(), payload, now);
        return existingContext.getId();
    }

    private String buildDefaultTitle(Long id) {
        return id == null ? "会话" : "会话 " + id;
    }

    private String serializeMessages(List<DeepSeekClient.Message> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            throw new IllegalStateException("序列化对话上下文失败", e);
        }
    }


    private List<ChatHistoryResponse.ChatHistoryMessage> toChatHistoryMessages(List<DeepSeekClient.Message> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatHistoryResponse.ChatHistoryMessage> result = new ArrayList<>();
        for (DeepSeekClient.Message message : history) {
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            String role = message.getRole();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            ChatHistoryResponse.ChatHistoryMessage dto = new ChatHistoryResponse.ChatHistoryMessage();
            dto.setType(role);
            dto.setContent(message.getContent());
            if ("assistant".equals(role)) {
                List<String> plugins = message.getPlugins();
                if (plugins != null && !plugins.isEmpty()) {
                    dto.setPlugins(new ArrayList<>(plugins));
                }
                dto.setRagContext(message.getRagContext());
            }
            result.add(dto);
        }
        return result;
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

    private record ConversationState(List<DeepSeekClient.Message> messages,
                                     AgentConversationContext existingContext) {}

    private record ModelAnswer(String content, List<String> pluginsUsed, Long sessionId, ChatResponse.RagContext ragContext) {
        private ModelAnswer {
            pluginsUsed = pluginsUsed == null ? Collections.emptyList() : List.copyOf(pluginsUsed);
        }
        
        // 便捷构造器，用于不需要 ragContext 的情况
        public ModelAnswer(String content, List<String> pluginsUsed, Long sessionId) {
            this(content, pluginsUsed, sessionId, null);
        }
    }

    private record ToolCallResult(String payload, String pluginName) {}

    private record RetrievalResult(String context, ChatResponse.RagContext ragContext) {}
}
