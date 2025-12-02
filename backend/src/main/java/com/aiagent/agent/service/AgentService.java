package com.aiagent.agent.service;

import com.aiagent.agent.dto.AgentResponse;
import com.aiagent.agent.dto.ChatHistoryResponse;
import com.aiagent.agent.dto.ChatRequest;
import com.aiagent.agent.dto.ChatResponse;
import com.aiagent.agent.dto.CreateAgentRequest;
import com.aiagent.agent.dto.TestAgentRequest;
import com.aiagent.agent.dto.TestAgentResponse;
import com.aiagent.agent.dto.UpdateAgentRequest;

import java.util.List;

public interface AgentService {

    AgentResponse createAgent(Long userId, CreateAgentRequest request);

    List<AgentResponse> getAgentsByUserId(Long userId);

    AgentResponse getAgentById(Long id, Long userId);

    AgentResponse updateAgent(Long id, Long userId, UpdateAgentRequest request);

    void deleteAgent(Long id, Long userId);

    TestAgentResponse testAgent(Long id, Long userId, TestAgentRequest request);

    /**
     * 发布智能体
     */
    AgentResponse publishAgent(Long id, Long userId);

    /**
     * 与智能体对话
     */
    ChatResponse chat(Long id, Long userId, ChatRequest request);

    /**
     * 获取对话历史
     */
    ChatHistoryResponse getConversation(Long id, Long userId);
}
