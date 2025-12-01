package com.aiagent.plugin.client;

/**
 * Represents the outcome of a plugin HTTP invocation.
 */
public class PluginInvocationResult {

    private final boolean success;
    private final int statusCode;
    private final String body;
    private final String errorMessage;

    private PluginInvocationResult(boolean success, int statusCode, String body, String errorMessage) {
        this.success = success;
        this.statusCode = statusCode;
        this.body = body;
        this.errorMessage = errorMessage;
    }

    public static PluginInvocationResult success(int statusCode, String body) {
        return new PluginInvocationResult(true, statusCode, body, null);
    }

    public static PluginInvocationResult failure(int statusCode, String body) {
        return new PluginInvocationResult(false, statusCode, body, null);
    }

    public static PluginInvocationResult failure(String errorMessage) {
        return new PluginInvocationResult(false, -1, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
