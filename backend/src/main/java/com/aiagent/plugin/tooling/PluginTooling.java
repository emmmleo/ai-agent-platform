package com.aiagent.plugin.tooling;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.aiagent.agent.client.DeepSeekClient;

/**
 * Aggregated tool payload plus runtime metadata for plugin operations.
 */
public class PluginTooling {

    private final List<DeepSeekClient.Tool> tools;
    private final Map<String, PluginOperationDescriptor> operationRegistry;

    public PluginTooling(List<DeepSeekClient.Tool> tools,
                         Map<String, PluginOperationDescriptor> operationRegistry) {
        this.tools = tools == null ? Collections.emptyList() : Collections.unmodifiableList(tools);
        this.operationRegistry = operationRegistry == null ? Collections.emptyMap() : Collections.unmodifiableMap(operationRegistry);
    }

    public List<DeepSeekClient.Tool> getTools() {
        return tools;
    }

    public Map<String, PluginOperationDescriptor> getOperationRegistry() {
        return operationRegistry;
    }
}
