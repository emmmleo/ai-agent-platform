package com.aiagent.plugin.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * OpenAPI规范验证器
 */
public class OpenAPIValidator {

    private static final Logger log = LoggerFactory.getLogger(OpenAPIValidator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证OpenAPI规范格式
     */
    public static void validate(String openapiJson) {
        if (openapiJson == null || openapiJson.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAPI规范不能为空");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(openapiJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("OpenAPI规范格式错误：不是有效的JSON格式 - " + e.getMessage());
        }

        // 检查必需字段
        if (!root.has("openapi") && !root.has("swagger")) {
            throw new IllegalArgumentException("OpenAPI规范必须包含 'openapi' 或 'swagger' 字段");
        }

        // 检查版本
        String version = root.has("openapi") ? root.get("openapi").asText() : root.get("swagger").asText();
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAPI规范版本不能为空");
        }

        // 检查info字段
        if (!root.has("info")) {
            throw new IllegalArgumentException("OpenAPI规范必须包含 'info' 字段");
        }

        JsonNode info = root.get("info");
        if (!info.has("title")) {
            throw new IllegalArgumentException("OpenAPI规范的 'info' 字段必须包含 'title'");
        }

        if (!info.has("version")) {
            throw new IllegalArgumentException("OpenAPI规范的 'info' 字段必须包含 'version'");
        }

        // 检查paths字段（可选但推荐）
        if (!root.has("paths")) {
            log.warn("OpenAPI规范缺少 'paths' 字段，插件可能无法正常工作");
        } else {
            JsonNode paths = root.get("paths");
            if (!paths.isObject() || paths.size() == 0) {
                log.warn("OpenAPI规范的 'paths' 字段为空，插件可能无法正常工作");
            } else {
                // 验证每个path
                Iterator<String> fieldNames = paths.fieldNames();
                while (fieldNames.hasNext()) {
                    String path = fieldNames.next();
                    JsonNode pathItem = paths.get(path);
                    
                    if (!pathItem.isObject()) {
                        throw new IllegalArgumentException("OpenAPI规范的路径 '" + path + "' 格式错误");
                    }
                    
                    // 检查是否有HTTP方法（get, post, put, delete等）
                    boolean hasMethod = false;
                    for (String method : new String[]{"get", "post", "put", "delete", "patch", "head", "options"}) {
                        if (pathItem.has(method)) {
                            hasMethod = true;
                            break;
                        }
                    }
                    
                    if (!hasMethod) {
                        log.warn("OpenAPI规范的路径 '" + path + "' 没有定义HTTP方法");
                    }
                }
            }
        }

        // 检查servers字段（可选）
        if (root.has("servers")) {
            JsonNode servers = root.get("servers");
            if (!servers.isArray()) {
                throw new IllegalArgumentException("OpenAPI规范的 'servers' 字段必须是数组");
            }
        }

        log.info("OpenAPI规范验证通过，版本: {}, 标题: {}", version, info.get("title").asText());
    }
}

