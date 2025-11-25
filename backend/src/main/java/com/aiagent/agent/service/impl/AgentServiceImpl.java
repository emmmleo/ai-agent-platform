package com.aiagent.agent.service.impl;

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
import com.aiagent.workflow.service.WorkflowExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private static final String DEFAULT_STATUS = "draft";

    private final AgentMapper agentMapper;
    private final WorkflowExecutionService workflowExecutionService;
    private final ObjectMapper objectMapper;

    public AgentServiceImpl(AgentMapper agentMapper,
                           WorkflowExecutionService workflowExecutionService,
                           ObjectMapper objectMapper) {
        this.agentMapper = agentMapper;
        this.workflowExecutionService = workflowExecutionService;
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

        // TODO: 这里应该调用实际的AI模型API
        // 目前返回模拟的回答
        String answer = generateMockAnswer(agent.getSystemPrompt(), request.getQuestion());
        
        return new TestAgentResponse(answer);
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
                    } else {
                        // 工作流执行失败，回退到直接调用AI
                        answer = callAIModel(agent, request.getQuestion(), context);
                    }
                } catch (Exception e) {
                    log.warn("工作流执行失败: {}", e.getMessage());
                    // 工作流执行失败，回退到直接调用AI
                    answer = callAIModel(agent, request.getQuestion(), context);
                }
            } else {
                // 3. 直接调用AI模型
                answer = callAIModel(agent, request.getQuestion(), context);
            }

            return new ChatResponse(answer, source);
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
    private String callAIModel(Agent agent, String question, String context) {
        // TODO: 实现实际的AI模型调用
        // 1. 解析 modelConfig 获取模型配置（如OpenAI、Claude等）
        // 2. 构建提示词：
        //    - 系统提示词：agent.getSystemPrompt()
        //    - 上下文（如果有）：context
        //    - 用户问题：question
        // 3. 调用AI模型API
        // 4. 返回生成的回答

        String systemPrompt = agent.getSystemPrompt() != null ? agent.getSystemPrompt() : "";
        String modelConfig = agent.getModelConfig() != null ? agent.getModelConfig() : "{}";

        log.info("调用AI模型：系统提示词长度: {}, 模型配置: {}, 问题: {}", 
                systemPrompt.length(), modelConfig, question);

        // 构建完整提示
        StringBuilder fullPrompt = new StringBuilder();
        if (!systemPrompt.isEmpty()) {
            fullPrompt.append("系统提示：").append(systemPrompt).append("\n\n");
        }
        if (!context.isEmpty()) {
            fullPrompt.append("上下文：").append(context).append("\n\n");
        }
        fullPrompt.append("用户问题：").append(question);

        // 模拟AI回答
        String promptPreview = systemPrompt.length() > 50 
                ? systemPrompt.substring(0, 50) + "..." 
                : systemPrompt;
        
        return String.format("基于系统提示词：%s\n\n问题：%s\n\n回答：这是一个模拟回答。实际应用中，这里会调用AI模型（如OpenAI、Claude等）来生成回答。\n\n提示：请配置 modelConfig 并集成实际的AI模型API。", 
                promptPreview, question);
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
        // 模拟AI回答，实际应该调用AI模型API
        String promptPreview = systemPrompt != null && systemPrompt.length() > 50 
                ? systemPrompt.substring(0, 50) + "..." 
                : (systemPrompt != null ? systemPrompt : "无");
        return String.format("基于系统提示词：%s\n\n问题：%s\n\n回答：这是一个模拟回答。实际应用中，这里会调用AI模型（如OpenAI、Claude等）来生成回答。", 
                promptPreview, question);
    }
}
