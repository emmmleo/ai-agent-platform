package com.aiagent.knowledgebase.service;

import com.aiagent.knowledgebase.dto.KnowledgeDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文档服务接口
 */
public interface KnowledgeDocumentService {

    /**
     * 上传文档到知识库
     */
    KnowledgeDocumentResponse uploadDocument(Long knowledgeBaseId, Long userId, MultipartFile file);

    /**
     * 获取知识库的所有文档
     */
    List<KnowledgeDocumentResponse> getDocumentsByKnowledgeBaseId(Long knowledgeBaseId, Long userId);

    /**
     * 根据ID获取文档详情
     */
    KnowledgeDocumentResponse getDocumentById(Long id, Long userId);

    /**
     * 删除文档
     */
    void deleteDocument(Long id, Long userId);
}

