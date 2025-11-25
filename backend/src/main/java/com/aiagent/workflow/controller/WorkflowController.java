package com.aiagent.workflow.controller;

import com.aiagent.workflow.dto.CreateWorkflowRequest;
import com.aiagent.workflow.dto.WorkflowResponse;
import com.aiagent.workflow.service.WorkflowService;
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
 * 工作流控制器
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowService workflowService;
    private final UserService userService;

    public WorkflowController(WorkflowService workflowService, UserService userService) {
        this.workflowService = workflowService;
        this.userService = userService;
    }

    /**
     * 创建工作流
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        WorkflowResponse workflow = workflowService.createWorkflow(userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "工作流创建成功");
        response.put("data", workflow);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取当前用户的工作流列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWorkflows(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<WorkflowResponse> workflows = workflowService.getWorkflowsByUserId(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", workflows);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取工作流详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWorkflow(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        WorkflowResponse workflow = workflowService.getWorkflowById(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", workflow);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新工作流
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody CreateWorkflowRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        WorkflowResponse workflow = workflowService.updateWorkflow(id, userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "工作流更新成功");
        response.put("data", workflow);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除工作流
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteWorkflow(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        workflowService.deleteWorkflow(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "工作流删除成功");
        
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        return user.getId();
    }
}

