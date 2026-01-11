package com.aiagent.dashboard.controller;

import com.aiagent.dashboard.dto.DashboardStatsResponse;
import com.aiagent.dashboard.service.DashboardService;
import com.aiagent.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final JwtUtils jwtUtils;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(HttpServletRequest request) {
        Long userId = jwtUtils.getUserIdFromRequest(request);
        return ResponseEntity.ok(dashboardService.getStats(userId));
    }
}
