package com.aiagent.plugin.service;

import com.aiagent.plugin.dto.PluginResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 插件服务接口
 */
public interface PluginService {

    /**
     * 注册插件（上传OpenAPI规范文件）
     */
    PluginResponse registerPlugin(Long userId, String name, String description, MultipartFile openapiFile);

    /**
     * 获取用户的所有插件
     */
    List<PluginResponse> getPluginsByUserId(Long userId);

    /**
     * 获取所有启用的插件
     */
    List<PluginResponse> getEnabledPlugins();

    /**
     * 根据ID获取插件详情
     */
    PluginResponse getPluginById(Long id, Long userId);

    /**
     * 更新插件信息
     */
    PluginResponse updatePlugin(Long id, Long userId, String name, String description, MultipartFile openapiFile);

    /**
     * 启用/禁用插件
     */
    PluginResponse togglePlugin(Long id, Long userId, Boolean enabled);

    /**
     * 删除插件
     */
    void deletePlugin(Long id, Long userId);
}

