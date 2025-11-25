package com.aiagent.knowledgebase.mapper;

import com.aiagent.knowledgebase.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper {

    /**
     * 根据用户ID查询所有知识库
     */
    List<KnowledgeBase> findByUserId(@Param("userId") Long userId);

    /**
     * 根据ID查询知识库
     */
    KnowledgeBase findById(@Param("id") Long id);

    /**
     * 根据ID和用户ID查询知识库（确保权限）
     */
    KnowledgeBase findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 插入知识库
     */
    int insert(KnowledgeBase knowledgeBase);

    /**
     * 更新知识库
     */
    int update(KnowledgeBase knowledgeBase);

    /**
     * 删除知识库
     */
    int deleteById(@Param("id") Long id);
}

