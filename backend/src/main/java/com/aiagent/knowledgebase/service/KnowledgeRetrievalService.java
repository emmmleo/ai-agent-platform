package com.aiagent.knowledgebase.service;

import com.aiagent.knowledgebase.util.VectorStoreService;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索服务接口
 */
public interface KnowledgeRetrievalService {

    /**
     * 知识库检索结果项
     */
    record RetrievalResultItem(
            String content,       // 文档内容
            Map<String, Object> metadata,  // 元数据
            double score          // 相似度分数
    ) {}

    /**
     * 根据查询文本检索相关文档
     *
     * @param knowledgeBaseIds 知识库ID列表
     * @param query            查询文本
     * @param topK             返回结果数量，默认3
     * @param minScore         最小相似度分数，默认0.0
     * @return 检索结果列表
     */
    List<RetrievalResultItem> retrieve(List<Long> knowledgeBaseIds, String query, Integer topK, Double minScore);

    /**
     * 根据查询文本检索相关文档（使用默认参数）
     *
     * @param knowledgeBaseIds 知识库ID列表
     * @param query            查询文本
     * @return 检索结果列表
     */
    default List<RetrievalResultItem> retrieve(List<Long> knowledgeBaseIds, String query) {
        return retrieve(knowledgeBaseIds, query, 3, 0.0);
    }
}