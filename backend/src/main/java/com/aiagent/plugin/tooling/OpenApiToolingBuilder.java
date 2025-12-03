package com.aiagent.plugin.tooling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.aiagent.agent.client.DeepSeekClient;
import com.aiagent.plugin.entity.Plugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Convert stored OpenAPI specs to DeepSeek tool definitions and runtime metadata.
 */
@Component
public class OpenApiToolingBuilder {

    private static final Logger log = LoggerFactory.getLogger(OpenApiToolingBuilder.class);
    private static final Pattern INVALID_NAME_CHARS = Pattern.compile("[^a-zA-Z0-9_]");

    private final ObjectMapper objectMapper;

    public OpenApiToolingBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PluginTooling build(List<Plugin> plugins) {
        if (plugins == null || plugins.isEmpty()) {
            return new PluginTooling(List.of(), Map.of());
        }

        List<DeepSeekClient.Tool> tools = new ArrayList<>();
        Map<String, PluginOperationDescriptor> registry = new HashMap<>();

        for (Plugin plugin : plugins) {
            if (plugin == null || !Boolean.TRUE.equals(plugin.getEnabled())) {
                continue;
            }
            if (!StringUtils.hasText(plugin.getOpenapiSpec())) {
                log.debug("插件{}未配置OpenAPI规范，跳过", plugin.getId());
                continue;
            }
            try {
                JsonNode root = objectMapper.readTree(plugin.getOpenapiSpec());
                JsonNode paths = root.path("paths");
                if (paths == null || paths.isMissingNode() || !paths.fields().hasNext()) {
                    log.warn("插件{}未定义任何路径", plugin.getId());
                    continue;
                }
                Iterator<Map.Entry<String, JsonNode>> pathIterator = paths.fields();
                while (pathIterator.hasNext()) {
                    Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
                    String path = pathEntry.getKey();
                    JsonNode pathNode = pathEntry.getValue();
                    Iterator<Map.Entry<String, JsonNode>> methodIterator = pathNode.fields();
                    while (methodIterator.hasNext()) {
                        Map.Entry<String, JsonNode> methodEntry = methodIterator.next();
                        String httpMethod = methodEntry.getKey().toUpperCase();
                        JsonNode operation = methodEntry.getValue();
                        processOperation(plugin, root, path, pathNode, httpMethod, operation, tools, registry);
                    }
                }
            } catch (Exception ex) {
                log.warn("解析插件OpenAPI失败 pluginId={}", plugin.getId(), ex);
            }
        }
        return new PluginTooling(tools, registry);
    }

    private void processOperation(Plugin plugin,
                                  JsonNode root,
                                  String path,
                                  JsonNode pathNode,
                                  String httpMethod,
                                  JsonNode operation,
                                  List<DeepSeekClient.Tool> tools,
                                  Map<String, PluginOperationDescriptor> registry) {
        String operationId = resolveOperationId(plugin, operation, httpMethod, path);
        if (!StringUtils.hasText(operationId)) {
            log.debug("插件{}在路径{}方法{}缺少operationId，跳过", plugin.getId(), path, httpMethod);
            return;
        }
        String functionName = buildFunctionName(plugin.getId(), operationId);
        String description = resolveDescription(operation);
        ObjectNode parameterSchema = buildParameterSchema(operation);
        if (parameterSchema == null) {
            parameterSchema = objectMapper.createObjectNode();
            parameterSchema.put("type", "object");
            parameterSchema.set("properties", objectMapper.createObjectNode());
            parameterSchema.set("required", objectMapper.createArrayNode());
        }

        DeepSeekClient.Function function = new DeepSeekClient.Function();
        function.setName(functionName);
        function.setDescription(description);
        function.setStrict(Boolean.TRUE);
        function.setParameters(parameterSchema);

        DeepSeekClient.Tool tool = new DeepSeekClient.Tool();
        tool.setType("function");
        tool.setFunction(function);
        tools.add(tool);

        List<String> pathParams = extractParamNames(operation, "path");
        List<String> queryParams = extractParamNames(operation, "query");
        boolean hasBody = operation.has("requestBody");
        String serverUrl = resolveServerUrl(root, pathNode, operation);
        if (!StringUtils.hasText(serverUrl)) {
            log.warn("插件{}的operation {} 缺少服务器地址，无法执行", plugin.getId(), operationId);
        }
        String contentType = resolveRequestContentType(operation);

        registry.put(functionName, new PluginOperationDescriptor(
                plugin.getId(),
                plugin.getName(),
                serverUrl,
                httpMethod,
                path,
                contentType,
                pathParams,
                queryParams,
                hasBody));
    }

    private ObjectNode buildParameterSchema(JsonNode operation) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = objectMapper.createObjectNode();
        ArrayNode required = objectMapper.createArrayNode();
        boolean hasContent = false;

        JsonNode parametersNode = operation.path("parameters");
        if (parametersNode.isArray()) {
            hasContent = true;
            for (JsonNode param : parametersNode) {
                String name = param.path("name").asText();
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                boolean isRequired = param.path("required").asBoolean(false);
                if (isRequired) {
                    required.add(name);
                }
                ObjectNode property = buildPropertySchema(param.path("schema"));
                if (param.has("description")) {
                    property.put("description", param.path("description").asText());
                }
                properties.set(name, property);
            }
        }

        JsonNode requestBody = operation.path("requestBody");
        if (requestBody != null && !requestBody.isMissingNode()) {
            JsonNode schemaNode = extractRequestBodySchema(requestBody);
            if (schemaNode != null) {
                hasContent = true;
                if (schemaNode.has("properties")) {
                    JsonNode props = schemaNode.get("properties");
                    if (props.isObject()) {
                        props.fields().forEachRemaining(entry -> properties.set(entry.getKey(), entry.getValue().deepCopy()));
                    }
                }
                if (schemaNode.has("required")) {
                    schemaNode.get("required").forEach(req -> required.add(req.asText()));
                }
            }
        }

        if (!hasContent) {
            return null;
        }
        schema.set("properties", properties);
        schema.set("required", required);
        return schema;
    }

    private ObjectNode buildPropertySchema(JsonNode schemaNode) {
        ObjectNode property = objectMapper.createObjectNode();
        if (schemaNode != null && schemaNode.isObject()) {
            property.setAll((ObjectNode) schemaNode.deepCopy());
        }
        if (!property.has("type")) {
            property.put("type", schemaNode.path("type").asText("string"));
        }
        return property;
    }

    private List<String> extractParamNames(JsonNode operation, String location) {
        List<String> names = new ArrayList<>();
        JsonNode parametersNode = operation.path("parameters");
        if (!parametersNode.isArray()) {
            return names;
        }
        for (JsonNode param : parametersNode) {
            if (location.equals(param.path("in").asText())) {
                String name = param.path("name").asText();
                if (StringUtils.hasText(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private JsonNode extractRequestBodySchema(JsonNode requestBody) {
        JsonNode content = requestBody.path("content");
        if (!content.isObject()) {
            return null;
        }
        JsonNode jsonNode = content.path("application/json");
        if (jsonNode.isMissingNode()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = content.fields();
            if (iterator.hasNext()) {
                jsonNode = iterator.next().getValue();
            }
        }
        return jsonNode != null ? jsonNode.path("schema") : null;
    }

    private String resolveRequestContentType(JsonNode operation) {
        JsonNode content = operation.path("requestBody").path("content");
        if (!content.isObject() || !content.fields().hasNext()) {
            return "application/json";
        }
        Map.Entry<String, JsonNode> first = content.fields().next();
        return first.getKey();
    }

    private String resolveServerUrl(JsonNode root, JsonNode pathNode, JsonNode operation) {
        String opServer = pickServer(operation.path("servers"));
        if (StringUtils.hasText(opServer)) {
            return opServer;
        }
        String pathServer = pickServer(pathNode.path("servers"));
        if (StringUtils.hasText(pathServer)) {
            return pathServer;
        }
        return pickServer(root.path("servers"));
    }

    private String pickServer(JsonNode serversNode) {
        if (serversNode != null && serversNode.isArray()) {
            for (JsonNode server : serversNode) {
                String url = server.path("url").asText(null);
                if (StringUtils.hasText(url)) {
                    return url;
                }
            }
        }
        return null;
    }

    private String resolveDescription(JsonNode operation) {
        String description = operation.path("description").asText(null);
        if (!StringUtils.hasText(description)) {
            description = operation.path("summary").asText(null);
        }
        if (!StringUtils.hasText(description)) {
            description = "插件函数调用";
        }
        return description;
    }

    private String resolveOperationId(Plugin plugin, JsonNode operation, String httpMethod, String path) {
        String opId = operation.path("operationId").asText(null);
        if (StringUtils.hasText(opId)) {
            return opId;
        }
        String fallback = (plugin.getName() != null ? plugin.getName() : "plugin")
                + "_" + httpMethod + "_" + path.replace('/', '_');
        return INVALID_NAME_CHARS.matcher(fallback).replaceAll("_");
    }

    private String buildFunctionName(Long pluginId, String operationId) {
        String normalized = INVALID_NAME_CHARS.matcher(operationId).replaceAll("_");
        return "plugin_" + pluginId + "_" + normalized;
    }
}
