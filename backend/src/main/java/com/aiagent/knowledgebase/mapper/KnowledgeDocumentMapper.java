package com.aiagent.knowledgebase.mapper;

import com.aiagent.knowledgebase.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {

    /**
     * 根据知识库ID查询所有文档
     */
    List<KnowledgeDocument> findByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
     * 根据ID查询文档
     */
    KnowledgeDocument findById(@Param("id") Long id);

    /**
     * 根据ID和用户ID查询文档（确保权限）
     */
    KnowledgeDocument findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 插入文档
     */
    int insert(KnowledgeDocument document);

    /**
     * 更新文档
     */
    int update(KnowledgeDocument document);

    /**
     * 删除文档
     */
    int deleteById(@Param("id") Long id);
}

