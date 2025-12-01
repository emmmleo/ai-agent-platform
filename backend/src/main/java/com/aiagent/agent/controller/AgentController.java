package com.aiagent.agent.controller;

import com.aiagent.agent.dto.AgentResponse;
import com.aiagent.agent.dto.ChatRequest;
import com.aiagent.agent.dto.ChatResponse;
import com.aiagent.agent.dto.CreateAgentRequest;
import com.aiagent.agent.dto.TestAgentRequest;
import com.aiagent.agent.dto.TestAgentResponse;
import com.aiagent.agent.dto.UpdateAgentRequest;
import com.aiagent.agent.service.AgentService;
import com.aiagent.user.entity.User;
import com.aiagent.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 智能体控制器
 */
@RestController
@RequestMapping("/v1/agents")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;
    private final UserService userService;

    public AgentController(AgentService agentService, UserService userService) {
        this.agentService = agentService;
        this.userService = userService;
    }

    /**
     * 创建智能体
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAgent(
            @Valid @RequestBody CreateAgentRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        AgentResponse agent = agentService.createAgent(userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "智能体创建成功");
        response.put("data", agent);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取当前用户的智能体列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAgents(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            log.debug("Fetching agents for user: {}", userId);
            List<AgentResponse> agents = agentService.getAgentsByUserId(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", agents);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching agents", e);
            throw e;
        }
    }

    /**
     * 获取智能体详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAgent(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        AgentResponse agent = agentService.getAgentById(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", agent);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新智能体
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAgent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAgentRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        AgentResponse agent = agentService.updateAgent(id, userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "智能体更新成功");
        response.put("data", agent);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除智能体
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAgent(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        agentService.deleteAgent(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "智能体删除成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 测试智能体
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testAgent(
            @PathVariable Long id,
            @Valid @RequestBody TestAgentRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        TestAgentResponse response = agentService.testAgent(id, userId, request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", response);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 发布智能体
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishAgent(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        AgentResponse agent = agentService.publishAgent(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "智能体发布成功");
        response.put("data", agent);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 与智能体对话
     */
    @PostMapping("/{id}/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @PathVariable Long id,
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        ChatResponse chatResponse = agentService.chat(id, userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", chatResponse);
        
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        return user.getId();
    }
}

