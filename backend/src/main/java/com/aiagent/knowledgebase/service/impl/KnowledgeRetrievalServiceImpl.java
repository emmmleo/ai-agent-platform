package com.aiagent.knowledgebase.service.impl;

import com.aiagent.knowledgebase.service.KnowledgeRetrievalService;
import com.aiagent.knowledgebase.util.VectorStoreService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库检索服务实现类
 */
@Service
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private final VectorStoreService vectorStoreService;

    public KnowledgeRetrievalServiceImpl(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    public List<RetrievalResultItem> retrieve(List<Long> knowledgeBaseIds, String query, Integer topK, Double minScore) {
        // 调用VectorStoreService进行检索
        List<VectorStoreService.ChunkSearchResult> searchResults = vectorStoreService.search(knowledgeBaseIds, query, topK);

        // 转换为KnowledgeRetrievalService.RetrievalResultItem格式
        return searchResults.stream()
                .map(result -> {
                    // 创建元数据Map
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("knowledgeBaseId", result.knowledgeBaseId());
                    metadata.put("documentId", result.documentId());
                    metadata.put("chunkIndex", result.chunkIndex());

                    return new RetrievalResultItem(
                            result.content(),
                            metadata,
                            result.score()
                    );
                })
                .collect(Collectors.toList());
    }
}