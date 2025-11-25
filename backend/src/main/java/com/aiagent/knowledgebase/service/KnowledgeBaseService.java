package com.aiagent.knowledgebase.service;

import com.aiagent.knowledgebase.dto.CreateKnowledgeBaseRequest;
import com.aiagent.knowledgebase.dto.KnowledgeBaseResponse;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     */
    KnowledgeBaseResponse createKnowledgeBase(Long userId, CreateKnowledgeBaseRequest request);

    /**
     * 获取用户的所有知识库
     */
    List<KnowledgeBaseResponse> getKnowledgeBasesByUserId(Long userId);

    /**
     * 根据ID获取知识库详情
     */
    KnowledgeBaseResponse getKnowledgeBaseById(Long id, Long userId);

    /**
     * 更新知识库
     */
    KnowledgeBaseResponse updateKnowledgeBase(Long id, Long userId, CreateKnowledgeBaseRequest request);

    /**
     * 删除知识库
     */
    void deleteKnowledgeBase(Long id, Long userId);
}

