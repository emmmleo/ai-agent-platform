package com.aiagent.dashboard.service.impl;

import com.aiagent.agent.mapper.AgentMapper;
import com.aiagent.dashboard.dto.DashboardStatsResponse;
import com.aiagent.dashboard.service.DashboardService;
import com.aiagent.knowledgebase.mapper.KnowledgeBaseMapper;
import com.aiagent.plugin.mapper.PluginMapper;
import com.aiagent.workflow.mapper.WorkflowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AgentMapper agentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final WorkflowMapper workflowMapper;
    private final PluginMapper pluginMapper;

    @Override
    public DashboardStatsResponse getStats(Long userId) {
        DashboardStatsResponse response = new DashboardStatsResponse();
        response.setAgentCount(agentMapper.countByUserId(userId));
        response.setKnowledgeBaseCount(knowledgeBaseMapper.countByUserId(userId));
        response.setWorkflowCount(workflowMapper.countByUserId(userId));
        response.setPluginCount(pluginMapper.countByUserId(userId));
        return response;
    }
}
