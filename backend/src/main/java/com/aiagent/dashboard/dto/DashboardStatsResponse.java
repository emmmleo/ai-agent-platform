package com.aiagent.dashboard.dto;

import lombok.Data;

@Data
public class DashboardStatsResponse {
    private int agentCount;
    private int knowledgeBaseCount;
    private int workflowCount;
    private int pluginCount;
}
