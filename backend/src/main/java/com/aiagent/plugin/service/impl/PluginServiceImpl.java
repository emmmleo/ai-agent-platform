package com.aiagent.plugin.service.impl;

import com.aiagent.plugin.dto.PluginResponse;
import com.aiagent.plugin.entity.Plugin;
import com.aiagent.plugin.mapper.PluginMapper;
import com.aiagent.plugin.service.PluginService;
import com.aiagent.plugin.util.OpenAPIValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PluginServiceImpl implements PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginServiceImpl.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final PluginMapper pluginMapper;
    private final ObjectMapper objectMapper;

    public PluginServiceImpl(PluginMapper pluginMapper, ObjectMapper objectMapper) {
        this.pluginMapper = pluginMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PluginResponse registerPlugin(Long userId, String name, String description, MultipartFile openapiFile) {
        // 验证文件
        validateFile(openapiFile);

        // 读取文件内容
        String openapiJson;
        try {
            openapiJson = new String(openapiFile.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取OpenAPI文件失败", e);
            throw new IllegalArgumentException("读取文件失败: " + e.getMessage());
        }

        // 验证OpenAPI规范格式
        OpenAPIValidator.validate(openapiJson);

        // 创建插件记录
        LocalDateTime now = LocalDateTime.now();
        Plugin plugin = new Plugin();
        plugin.setUserId(userId);
        plugin.setName(name);
        plugin.setDescription(description);
        plugin.setOpenapiSpec(openapiJson);
        plugin.setEnabled(true); // 默认启用
        plugin.setCreatedAt(now);
        plugin.setUpdatedAt(now);

        pluginMapper.insert(plugin);

        return toResponse(plugin);
    }

    @Override
    public List<PluginResponse> getPluginsByUserId(Long userId) {
        List<Plugin> plugins = pluginMapper.findByUserId(userId);
        return plugins.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PluginResponse> getEnabledPlugins() {
        List<Plugin> plugins = pluginMapper.findEnabledPlugins();
        return plugins.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PluginResponse getPluginById(Long id, Long userId) {
        Plugin plugin = pluginMapper.findByIdAndUserId(id, userId);
        if (plugin == null) {
            throw new IllegalArgumentException("插件不存在或无权限访问");
        }
        return toResponse(plugin);
    }

    @Override
    @Transactional
    public PluginResponse updatePlugin(Long id, Long userId, String name, String description, MultipartFile openapiFile) {
        Plugin plugin = pluginMapper.findByIdAndUserId(id, userId);
        if (plugin == null) {
            throw new IllegalArgumentException("插件不存在或无权限访问");
        }

        plugin.setName(name);
        plugin.setDescription(description);

        // 如果提供了新文件，更新OpenAPI规范
        if (openapiFile != null && !openapiFile.isEmpty()) {
            validateFile(openapiFile);
            String openapiJson;
            try {
                openapiJson = new String(openapiFile.getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("读取OpenAPI文件失败", e);
                throw new IllegalArgumentException("读取文件失败: " + e.getMessage());
            }
            OpenAPIValidator.validate(openapiJson);
            plugin.setOpenapiSpec(openapiJson);
        }

        plugin.setUpdatedAt(LocalDateTime.now());
        pluginMapper.update(plugin);

        return toResponse(plugin);
    }

    @Override
    @Transactional
    public PluginResponse togglePlugin(Long id, Long userId, Boolean enabled) {
        Plugin plugin = pluginMapper.findByIdAndUserId(id, userId);
        if (plugin == null) {
            throw new IllegalArgumentException("插件不存在或无权限访问");
        }

        plugin.setEnabled(enabled);
        plugin.setUpdatedAt(LocalDateTime.now());
        pluginMapper.update(plugin);

        return toResponse(plugin);
    }

    @Override
    @Transactional
    public void deletePlugin(Long id, Long userId) {
        Plugin plugin = pluginMapper.findByIdAndUserId(id, userId);
        if (plugin == null) {
            throw new IllegalArgumentException("插件不存在或无权限访问");
        }
        pluginMapper.deleteById(id);
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 验证文件类型（JSON文件）
        String lowerFilename = originalFilename.toLowerCase();
        if (!lowerFilename.endsWith(".json")) {
            throw new IllegalArgumentException("只支持JSON格式的文件");
        }

        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过5MB");
        }
    }

    private PluginResponse toResponse(Plugin plugin) {
        // 将JSON字符串解析为对象
        Object openapiSpec = null;
        try {
            openapiSpec = objectMapper.readValue(plugin.getOpenapiSpec(), Object.class);
        } catch (Exception e) {
            log.warn("解析OpenAPI规范失败: {}", plugin.getId(), e);
        }

        return new PluginResponse(
                plugin.getId(),
                plugin.getUserId(),
                plugin.getName(),
                plugin.getDescription(),
                openapiSpec,
                plugin.getEnabled(),
                plugin.getCreatedAt(),
                plugin.getUpdatedAt()
        );
    }
}

