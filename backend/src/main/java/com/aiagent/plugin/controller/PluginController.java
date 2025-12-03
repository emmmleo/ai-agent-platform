package com.aiagent.plugin.controller;

import com.aiagent.plugin.dto.PluginResponse;
import com.aiagent.plugin.service.PluginService;
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
 * 插件控制器
 */
@RestController
@RequestMapping("/v1/plugins")
public class PluginController {

    private static final Logger log = LoggerFactory.getLogger(PluginController.class);

    private final PluginService pluginService;
    private final UserService userService;

    public PluginController(PluginService pluginService, UserService userService) {
        this.pluginService = pluginService;
        this.userService = userService;
    }

    /**
     * 注册插件
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerPlugin(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("openapiFile") MultipartFile openapiFile,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            PluginResponse plugin = pluginService.registerPlugin(userId, name, description, openapiFile);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "插件注册成功");
            response.put("data", plugin);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("插件注册失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("插件注册异常", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "插件注册失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取当前用户的插件列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPlugins(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<PluginResponse> plugins = pluginService.getPluginsByUserId(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", plugins);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有启用的插件
     */
    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Object>> getEnabledPlugins() {
        List<PluginResponse> plugins = pluginService.getEnabledPlugins();
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", plugins);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取插件详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPlugin(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        PluginResponse plugin = pluginService.getPluginById(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", plugin);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新插件
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePlugin(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "openapiFile", required = false) MultipartFile openapiFile,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            PluginResponse plugin = pluginService.updatePlugin(id, userId, name, description, openapiFile);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "插件更新成功");
            response.put("data", plugin);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("插件更新失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("插件更新异常", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "插件更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 启用/禁用插件
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> togglePlugin(
            @PathVariable Long id,
            @RequestParam("enabled") Boolean enabled,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        PluginResponse plugin = pluginService.togglePlugin(id, userId, enabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", enabled ? "插件已启用" : "插件已禁用");
        response.put("data", plugin);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除插件
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePlugin(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        pluginService.deletePlugin(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "插件删除成功");
        
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        return user.getId();
    }
}

