package com.aiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gemini Embedding 配置
 */
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /**
     * 是否启用 Gemini Embedding
     */
    private boolean enabled = true;

    /**
     * Gemini API Key
     */
    private String apiKey;

    /**
     * Embedding 模型名称
     */
    private String embeddingModel = "gemini-embedding-001";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
}
