package com.aiagent.agent.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import com.aiagent.config.LlmProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lightweight DeepSeek chat-completion client.
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final LlmProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(LlmProperties properties, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
    }

    public String chat(String systemPrompt, String context, String question, Map<String, Object> modelConfig) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(new Message("system", systemPrompt));
        }
        StringBuilder userContent = new StringBuilder();
        if (StringUtils.hasText(context)) {
            userContent.append("相关上下文：").append(context).append("\n\n");
        }
        userContent.append(question);
        messages.add(new Message("user", userContent.toString()));

        ChatCompletionResponse response = chat(messages, null, modelConfig);
        Message message = extractFirstMessage(response);
        if (message == null || !StringUtils.hasText(message.getContent())) {
            throw new IllegalStateException("LLM响应缺少内容");
        }
        return message.getContent();
    }

    public ChatCompletionResponse chat(List<Message> messages, List<Tool> tools, Map<String, Object> modelConfig) {
        ensureReady();

        ChatCompletionRequest payload = buildRequest(messages, tools, modelConfig);
        HttpHeaders headers = buildHeaders();
        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(
                    buildChatUri(),
                    HttpMethod.POST,
                    entity,
                    ChatCompletionResponse.class);

            ChatCompletionResponse body = response.getBody();
            if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
                throw new IllegalStateException("LLM响应为空");
            }
            return body;
        } catch (RestClientResponseException e) {
            String detail = extractErrorMessage(e.getResponseBodyAsString());
            log.error("DeepSeek API调用失败: status={}, body={}", e.getRawStatusCode(), detail);
            throw new RuntimeException("调用DeepSeek失败: " + detail, e);
        } catch (RestClientException e) {
            log.error("DeepSeek API调用异常", e);
            throw new RuntimeException("调用DeepSeek失败: " + e.getMessage(), e);
        }
    }

    private Message extractFirstMessage(ChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return null;
        }
        Choice firstChoice = response.getChoices().get(0);
        return firstChoice != null ? firstChoice.getMessage() : null;
    }

    private ChatCompletionRequest buildRequest(List<Message> messages, List<Tool> tools, Map<String, Object> modelConfig) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(resolveString(modelConfig, "model", "modelName", "model_id", "modelId"));
        request.setMessages(messages);
        request.setTools(tools);
        request.setStream(Boolean.FALSE);
        request.setTemperature(resolveDouble(modelConfig, "temperature"));
        request.setTopP(resolveDouble(modelConfig, "top_p", "topP"));
        request.setMaxTokens(resolveInteger(modelConfig, "max_tokens", "maxTokens"));
        request.setPresencePenalty(resolveDouble(modelConfig, "presence_penalty", "presencePenalty"));
        request.setFrequencyPenalty(resolveDouble(modelConfig, "frequency_penalty", "frequencyPenalty"));

        if (!StringUtils.hasText(request.getModel())) {
            request.setModel(properties.getModel());
        }
        return request;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        return headers;
    }

    private URI buildChatUri() {
        String base = properties.getBaseUrl();
        if (!StringUtils.hasText(base)) {
            base = "https://api.deepseek.com";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + CHAT_COMPLETIONS_PATH);
    }

    private void ensureReady() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("LLM功能已禁用");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("未配置 llm.api-key，请设置环境变量 DEEPSEEK_API_KEY");
        }
    }

    private String extractErrorMessage(String body) {
        if (!StringUtils.hasText(body)) {
            return "未知错误";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode errorNode = root.get("error");
            if (errorNode != null) {
                if (errorNode.has("message")) {
                    return errorNode.get("message").asText();
                }
                return errorNode.toString();
            }
            return body;
        } catch (Exception ex) {
            return body;
        }
    }

    private String resolveString(Map<String, Object> config, String... keys) {
        if (config == null) {
            return null;
        }
        for (String key : keys) {
            Object value = config.get(key);
            if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
                return stringValue;
            }
        }
        return null;
    }

    private Double resolveDouble(Map<String, Object> config, String... keys) {
        if (config == null) {
            return null;
        }
        for (String key : keys) {
            Object value = config.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
                try {
                    return Double.parseDouble(stringValue);
                } catch (NumberFormatException ignored) {
                    // ignore invalid number
                }
            }
        }
        return null;
    }

    private Integer resolveInteger(Map<String, Object> config, String... keys) {
        if (config == null) {
            return null;
        }
        for (String key : keys) {
            Object value = config.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
                try {
                    return Integer.parseInt(stringValue);
                } catch (NumberFormatException ignored) {
                    // ignore invalid number
                }
            }
        }
        return null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletionRequest {
        private String model;
        private List<Message> messages;
        private List<Tool> tools;
        private Boolean stream;
        private Double temperature;
        @JsonProperty("top_p")
        private Double topP;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        @JsonProperty("presence_penalty")
        private Double presencePenalty;
        @JsonProperty("frequency_penalty")
        private Double frequencyPenalty;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }

        public List<Tool> getTools() {
            return tools;
        }

        public void setTools(List<Tool> tools) {
            this.tools = tools;
        }

        public Boolean getStream() {
            return stream;
        }

        public void setStream(Boolean stream) {
            this.stream = stream;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Double getTopP() {
            return topP;
        }

        public void setTopP(Double topP) {
            this.topP = topP;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Double getPresencePenalty() {
            return presencePenalty;
        }

        public void setPresencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
        }

        public Double getFrequencyPenalty() {
            return frequencyPenalty;
        }

        public void setFrequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
        }
    }

    public static class Message {
        private String role;
        private String content;
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
        @JsonProperty("tool_call_id")
        private String toolCallId;
        private String name;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public Message(String role, String content, String toolCallId) {
            this.role = role;
            this.content = content;
            this.toolCallId = toolCallId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<ToolCall> getToolCalls() {
            return toolCalls;
        }

        public void setToolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }

        public String getToolCallId() {
            return toolCallId;
        }

        public void setToolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ChatCompletionResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }
    }

    public static class Choice {
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tool {
        private String type;
        private Function function;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Function getFunction() {
            return function;
        }

        public void setFunction(Function function) {
            this.function = function;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {
        private String name;
        private String description;
        private Boolean strict;
        private JsonNode parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getStrict() {
            return strict;
        }

        public void setStrict(Boolean strict) {
            this.strict = strict;
        }

        public JsonNode getParameters() {
            return parameters;
        }

        public void setParameters(JsonNode parameters) {
            this.parameters = parameters;
        }
    }

    public static class ToolCall {
        private String id;
        private String type;
        private FunctionCall function;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public FunctionCall getFunction() {
            return function;
        }

        public void setFunction(FunctionCall function) {
            this.function = function;
        }
    }

    public static class FunctionCall {
        private String name;
        private String arguments;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
}
