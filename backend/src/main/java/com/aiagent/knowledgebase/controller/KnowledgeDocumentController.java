package com.aiagent.knowledgebase.controller;

import com.aiagent.knowledgebase.dto.KnowledgeDocumentResponse;
import com.aiagent.knowledgebase.service.KnowledgeDocumentService;
import com.aiagent.user.entity.User;
import com.aiagent.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档控制器
 */
@RestController
@RequestMapping("/v1/knowledge-bases/{knowledgeBaseId}/documents")
public class KnowledgeDocumentController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentController.class);

    private final KnowledgeDocumentService documentService;
    private final UserService userService;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService, UserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    /**
     * 上传文档到知识库
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @PathVariable Long knowledgeBaseId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            KnowledgeDocumentResponse document = documentService.uploadDocument(knowledgeBaseId, userId, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "文档上传成功，正在处理中");
            response.put("data", document);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("文档上传失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("文档上传异常", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "文档上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取知识库的所有文档
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDocuments(
            @PathVariable Long knowledgeBaseId,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<KnowledgeDocumentResponse> documents = documentService.getDocumentsByKnowledgeBaseId(knowledgeBaseId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", documents);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(
            @PathVariable Long knowledgeBaseId,
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        KnowledgeDocumentResponse document = documentService.getDocumentById(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", document);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @PathVariable Long knowledgeBaseId,
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        documentService.deleteDocument(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "文档删除成功");
        
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        return user.getId();
    }
}

