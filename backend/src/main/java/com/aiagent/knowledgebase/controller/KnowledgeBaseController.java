package com.aiagent.knowledgebase.controller;

import com.aiagent.knowledgebase.dto.CreateKnowledgeBaseRequest;
import com.aiagent.knowledgebase.dto.KnowledgeBaseResponse;
import com.aiagent.knowledgebase.service.KnowledgeBaseService;
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
 * 知识库控制器
 */
@RestController
@RequestMapping("/v1/knowledge-bases")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final UserService userService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService, UserService userService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.userService = userService;
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createKnowledgeBase(
            @Valid @RequestBody CreateKnowledgeBaseRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        KnowledgeBaseResponse knowledgeBase = knowledgeBaseService.createKnowledgeBase(userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "知识库创建成功");
        response.put("data", knowledgeBase);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取当前用户的知识库列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getKnowledgeBases(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<KnowledgeBaseResponse> knowledgeBases = knowledgeBaseService.getKnowledgeBasesByUserId(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", knowledgeBases);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getKnowledgeBase(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        KnowledgeBaseResponse knowledgeBase = knowledgeBaseService.getKnowledgeBaseById(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", knowledgeBase);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateKnowledgeBase(
            @PathVariable Long id,
            @Valid @RequestBody CreateKnowledgeBaseRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        KnowledgeBaseResponse knowledgeBase = knowledgeBaseService.updateKnowledgeBase(id, userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "知识库更新成功");
        response.put("data", knowledgeBase);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteKnowledgeBase(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        knowledgeBaseService.deleteKnowledgeBase(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "知识库删除成功");
        
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        return user.getId();
    }
}

