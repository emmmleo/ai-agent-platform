package com.aiagent.plugin.tooling;

import java.util.List;

/**
 * Metadata required to execute a plugin operation referenced by DeepSeek function calling.
 */
public class PluginOperationDescriptor {

    private final Long pluginId;
    private final String pluginName;
    private final String serverUrl;
    private final String httpMethod;
    private final String path;
    private final String contentType;
    private final List<String> pathParameters;
    private final List<String> queryParameters;
    private final boolean requiresRequestBody;

    public PluginOperationDescriptor(Long pluginId,
                                     String pluginName,
                                     String serverUrl,
                                     String httpMethod,
                                     String path,
                                     String contentType,
                                     List<String> pathParameters,
                                     List<String> queryParameters,
                                     boolean requiresRequestBody) {
        this.pluginId = pluginId;
        this.pluginName = pluginName;
        this.serverUrl = serverUrl;
        this.httpMethod = httpMethod;
        this.path = path;
        this.contentType = contentType;
        this.pathParameters = pathParameters;
        this.queryParameters = queryParameters;
        this.requiresRequestBody = requiresRequestBody;
    }

    public Long getPluginId() {
        return pluginId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getContentType() {
        return contentType;
    }

    public List<String> getPathParameters() {
        return pathParameters;
    }

    public List<String> getQueryParameters() {
        return queryParameters;
    }

    public boolean isRequiresRequestBody() {
        return requiresRequestBody;
    }
}
