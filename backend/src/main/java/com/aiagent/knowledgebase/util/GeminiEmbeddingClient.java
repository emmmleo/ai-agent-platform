package com.aiagent.knowledgebase.util;

import com.aiagent.config.GeminiProperties;
import com.google.genai.Client;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gemini Embedding 客户端
 * 调用 Google Gemini API 生成文本向量
 */
@Component
public class GeminiEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);

    private final GeminiProperties properties;
    private volatile Client client;

    public GeminiEmbeddingClient(GeminiProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取或创建 Gemini Client（懒加载）
     */
    private Client getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    String apiKey = properties.getApiKey();
                    if (!StringUtils.hasText(apiKey)) {
                        throw new IllegalStateException("缺少 gemini.api-key 配置");
                    }
                    client = Client.builder().apiKey(apiKey).build();
                }
            }
        }
        return client;
    }

    /**
     * 为单个文本生成向量
     */
    public float[] embed(String text) {
        if (!properties.isEnabled()) {
            log.warn("Gemini Embedding 已禁用");
            return new float[0];
        }
        if (!StringUtils.hasText(text)) {
            return new float[0];
        }

        List<float[]> results = embedAll(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    /**
     * 为多个文本批量生成向量
     */
    public List<float[]> embedAll(List<String> texts) {
        if (!properties.isEnabled()) {
            log.warn("Gemini Embedding 已禁用");
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(texts)) {
            return Collections.emptyList();
        }

        try {
            Client geminiClient = getClient();
            String model = properties.getEmbeddingModel();
            
            List<float[]> result = new ArrayList<>();
            
            // Gemini API 每次调用支持单个内容，需要逐个处理
            for (String text : texts) {
                if (!StringUtils.hasText(text)) {
                    result.add(new float[0]);
                    continue;
                }
                
                // 配置输出维度为 768
                EmbedContentConfig config = EmbedContentConfig.builder()
                        .outputDimensionality(768)
                        .build();
                
                EmbedContentResponse response = geminiClient.models.embedContent(model, text, config);
                
                if (response == null) {
                    log.warn("Gemini Embedding 响应为空");
                    result.add(new float[0]);
                    continue;
                }
                
                // 获取 embeddings 列表
                List<ContentEmbedding> embeddings = response.embeddings().orElse(null);
                if (embeddings == null || embeddings.isEmpty()) {
                    log.warn("Gemini Embedding 返回空的 embeddings 列表");
                    result.add(new float[0]);
                    continue;
                }
                
                ContentEmbedding embedding = embeddings.get(0);
                List<Float> values = embedding.values().orElse(null);
                
                if (values == null || values.isEmpty()) {
                    result.add(new float[0]);
                    continue;
                }
                
                float[] vector = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    vector[i] = values.get(i);
                }
                result.add(vector);
            }
            
            log.debug("成功生成 {} 个向量", result.size());
            return result;
        } catch (Exception e) {
            log.error("调用 Gemini Embedding API 失败", e);
            throw new IllegalStateException("生成向量失败: " + e.getMessage(), e);
        }
    }
}
