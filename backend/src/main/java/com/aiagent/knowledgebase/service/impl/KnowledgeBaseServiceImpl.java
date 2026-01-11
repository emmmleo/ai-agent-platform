package com.aiagent.knowledgebase.service.impl;

import com.aiagent.knowledgebase.dto.CreateKnowledgeBaseRequest;
import com.aiagent.knowledgebase.dto.KnowledgeBaseResponse;
import com.aiagent.knowledgebase.entity.KnowledgeBase;
import com.aiagent.knowledgebase.mapper.KnowledgeBaseMapper;
import com.aiagent.knowledgebase.service.KnowledgeBaseService;
import com.aiagent.knowledgebase.util.VectorStoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final VectorStoreService vectorStoreService;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                    VectorStoreService vectorStoreService) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    @Transactional
    public KnowledgeBaseResponse createKnowledgeBase(Long userId, CreateKnowledgeBaseRequest request) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setCreatedAt(now);
        knowledgeBase.setUpdatedAt(now);

        knowledgeBaseMapper.insert(knowledgeBase);

        return toResponse(knowledgeBase);
    }

    @Override
    public List<KnowledgeBaseResponse> getKnowledgeBasesByUserId(Long userId) {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.findByUserId(userId);
        return knowledgeBases.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public KnowledgeBaseResponse getKnowledgeBaseById(Long id, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findByIdAndUserId(id, userId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在或无权限访问");
        }
        return toResponse(knowledgeBase);
    }

    @Override
    @Transactional
    public KnowledgeBaseResponse updateKnowledgeBase(Long id, Long userId, CreateKnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findByIdAndUserId(id, userId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在或无权限访问");
        }

        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());

        knowledgeBaseMapper.update(knowledgeBase);

        return toResponse(knowledgeBase);
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(Long id, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findByIdAndUserId(id, userId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在或无权限访问");
        }
        // 删除向量数据，防止残留占用存储
        vectorStoreService.deleteByKnowledgeBaseId(id);
        knowledgeBaseMapper.deleteById(id);
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase knowledgeBase) {
        return new KnowledgeBaseResponse(
                knowledgeBase.getId(),
                knowledgeBase.getUserId(),
                knowledgeBase.getName(),
                knowledgeBase.getDescription(),
                knowledgeBase.getCreatedAt(),
                knowledgeBase.getUpdatedAt()
        );
    }
}

