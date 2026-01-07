package com.aiagent.dashboard.service;

import com.aiagent.dashboard.dto.DashboardStatsResponse;

public interface DashboardService {
    DashboardStatsResponse getStats(Long userId);
}
