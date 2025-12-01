package com.aiagent.plugin.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.aiagent.plugin.tooling.PluginOperationDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Execute HTTP calls defined by plugin OpenAPI operations.
 */
@Component
public class PluginInvocationClient {

    private static final Logger log = LoggerFactory.getLogger(PluginInvocationClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PluginInvocationClient(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = objectMapper;
    }

    public PluginInvocationResult invoke(PluginOperationDescriptor descriptor, JsonNode arguments) {
        if (descriptor == null) {
            return PluginInvocationResult.failure("插件操作描述为空");
        }
        if (!StringUtils.hasText(descriptor.getServerUrl())) {
            return PluginInvocationResult.failure("插件服务器地址未配置");
        }
        HttpMethod httpMethod = resolveHttpMethod(descriptor.getHttpMethod());
        if (httpMethod == null) {
            return PluginInvocationResult.failure("不支持的HTTP方法: " + descriptor.getHttpMethod());
        }

        ObjectNode argsObject = prepareArgs(arguments);
        Map<String, String> pathParams = extractArgs(argsObject, descriptor.getPathParameters());
        Map<String, String> queryParams = extractArgs(argsObject, descriptor.getQueryParameters());

        String resolvedPath = resolvePath(descriptor.getPath(), pathParams);
        if (resolvedPath != null && resolvedPath.contains("{")) {
            return PluginInvocationResult.failure("缺少必需的路径参数，无法构建请求");
        }
        URI uri = buildUri(descriptor.getServerUrl(), resolvedPath, queryParams);

        boolean hasBody = descriptor.isRequiresRequestBody() && allowsBody(httpMethod);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<?> entity;
        if (hasBody) {
            MediaType mediaType = MediaType.APPLICATION_JSON;
            if (StringUtils.hasText(descriptor.getContentType())) {
                try {
                    mediaType = MediaType.parseMediaType(descriptor.getContentType());
                } catch (IllegalArgumentException ex) {
                    log.warn("插件{} 指定的Content-Type无效: {}", descriptor.getPluginId(), descriptor.getContentType());
                }
            }
            headers.setContentType(mediaType);
            entity = new HttpEntity<>(argsObject, headers);
        } else {
            entity = new HttpEntity<>(headers);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, httpMethod, entity, String.class);
            return PluginInvocationResult.success(response.getStatusCodeValue(), response.getBody());
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.warn("插件{} 调用失败 status={} body={}", descriptor.getPluginId(), ex.getRawStatusCode(), body);
            return PluginInvocationResult.failure(ex.getRawStatusCode(), body);
        } catch (RestClientException ex) {
            log.warn("插件{} 调用异常", descriptor.getPluginId(), ex);
            return PluginInvocationResult.failure(ex.getMessage());
        }
    }

    private HttpMethod resolveHttpMethod(String method) {
        if (!StringUtils.hasText(method)) {
            return null;
        }
        try {
            return HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean allowsBody(HttpMethod method) {
        return HttpMethod.POST.equals(method)
                || HttpMethod.PUT.equals(method)
                || HttpMethod.PATCH.equals(method)
                || HttpMethod.DELETE.equals(method);
    }

    private ObjectNode prepareArgs(JsonNode arguments) {
        if (arguments != null && arguments.isObject()) {
            return (ObjectNode) arguments.deepCopy();
        }
        return objectMapper.createObjectNode();
    }

    private Map<String, String> extractArgs(ObjectNode args, List<String> keys) {
        Map<String, String> values = new LinkedHashMap<>();
        if (keys == null || keys.isEmpty()) {
            return values;
        }
        for (String key : keys) {
            if (args.has(key)) {
                JsonNode valueNode = args.remove(key);
                if (valueNode != null && !valueNode.isNull()) {
                    values.put(key, valueNode.asText());
                }
            }
        }
        return values;
    }

    private String resolvePath(String pathTemplate, Map<String, String> values) {
        if (!StringUtils.hasText(pathTemplate) || values.isEmpty()) {
            return pathTemplate;
        }
        String resolved = pathTemplate;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String encoded = UriUtils.encodePathSegment(entry.getValue(), StandardCharsets.UTF_8);
            resolved = resolved.replace(placeholder, encoded);
        }
        return resolved;
    }

    private URI buildUri(String serverUrl, String path, Map<String, String> queryParams) {
        String base = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        String normalizedPath = path == null ? "" : path;
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        String uriWithoutQuery = UriComponentsBuilder
                .fromHttpUrl(base + normalizedPath)
                .build(true)
                .toUriString();

        if (queryParams == null || queryParams.isEmpty()) {
            return URI.create(uriWithoutQuery);
        }

        StringBuilder queryBuilder = new StringBuilder();
        for (Entry<String, String> entry : queryParams.entrySet()) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append('&');
            }
            String encodedKey = UriUtils.encodeQueryParam(entry.getKey(), StandardCharsets.UTF_8);
            String value = entry.getValue();
            String encodedValue = value != null
                    ? UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8)
                    : "";
            queryBuilder.append(encodedKey).append('=').append(encodedValue);
        }

        return URI.create(uriWithoutQuery + "?" + queryBuilder);
    }
}
