package com.aiagent.workflow.controller;

import com.aiagent.workflow.dto.ExecuteWorkflowRequest;
import com.aiagent.workflow.dto.WorkflowExecutionResponse;
import com.aiagent.workflow.service.WorkflowExecutionService;
import com.aiagent.user.entity.User;
import com.aiagent.user.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行控制器
 */
@RestController
@RequestMapping("/api/v1/workflows/{workflowId}/executions")
public class WorkflowExecutionController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionController.class);

    private final WorkflowExecutionService executionService;
    private final UserService userService;

    public WorkflowExecutionController(WorkflowExecutionService executionService, UserService userService) {
        this.executionService = executionService;
        this.userService = userService;
    }

    /**
     * 执行工作流
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> executeWorkflow(
            @PathVariable Long workflowId,
            @Valid @RequestBody ExecuteWorkflowRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        WorkflowExecutionResponse execution = executionService.executeWorkflow(workflowId, userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "工作流执行已启动");
        response.put("data", execution);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取工作流的执行记录
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getExecutions(
            @PathVariable Long workflowId,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<WorkflowExecutionResponse> executions = executionService.getExecutionsByWorkflowId(workflowId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", executions);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取执行记录详情
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<Map<String, Object>> getExecution(
            @PathVariable Long workflowId,
            @PathVariable Long executionId,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        WorkflowExecutionResponse execution = executionService.getExecutionById(executionId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", execution);
        
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        return user.getId();
    }
}

