package com.aiagent.knowledgebase.service.impl;

import com.aiagent.knowledgebase.dto.KnowledgeDocumentResponse;
import com.aiagent.knowledgebase.entity.KnowledgeBase;
import com.aiagent.knowledgebase.entity.KnowledgeDocument;
import com.aiagent.knowledgebase.mapper.KnowledgeBaseMapper;
import com.aiagent.knowledgebase.mapper.KnowledgeDocumentMapper;
import com.aiagent.knowledgebase.service.KnowledgeDocumentService;
import com.aiagent.knowledgebase.util.TextSplitter;
import com.aiagent.knowledgebase.util.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentServiceImpl.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final TextSplitter textSplitter;
    private final VectorStoreService vectorStoreService;

    public KnowledgeDocumentServiceImpl(KnowledgeDocumentMapper documentMapper,
                                        KnowledgeBaseMapper knowledgeBaseMapper,
                                        TextSplitter textSplitter,
                                        VectorStoreService vectorStoreService) {
        this.documentMapper = documentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.textSplitter = textSplitter;
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    @Transactional
    public KnowledgeDocumentResponse uploadDocument(Long knowledgeBaseId, Long userId, MultipartFile file) {
        // 验证知识库是否存在且属于当前用户
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findByIdAndUserId(knowledgeBaseId, userId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在或无权限访问");
        }

        // 验证文件
        validateFile(file);

        // 读取文件内容
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new IllegalArgumentException("读取文件失败: " + e.getMessage());
        }

        // 确定文件类型
        String fileType = determineFileType(file.getOriginalFilename());

        // 创建文档记录
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocument document = new KnowledgeDocument();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setUserId(userId);
        document.setFileName(file.getOriginalFilename());
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setContent(content);
        document.setStatus("processing");
        document.setChunkCount(0);
        document.setVectorized(false);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        documentMapper.insert(document);

        // 异步处理文档（分块和向量化）
        processDocumentAsync(document.getId());

        return toResponse(document);
    }

    @Override
    public List<KnowledgeDocumentResponse> getDocumentsByKnowledgeBaseId(Long knowledgeBaseId, Long userId) {
        // 验证知识库权限
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findByIdAndUserId(knowledgeBaseId, userId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在或无权限访问");
        }

        List<KnowledgeDocument> documents = documentMapper.findByKnowledgeBaseId(knowledgeBaseId);
        return documents.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public KnowledgeDocumentResponse getDocumentById(Long id, Long userId) {
        KnowledgeDocument document = documentMapper.findByIdAndUserId(id, userId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在或无权限访问");
        }
        return toResponse(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, Long userId) {
        KnowledgeDocument document = documentMapper.findByIdAndUserId(id, userId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在或无权限访问");
        }
        // 删除向量数据
        vectorStoreService.deleteByDocumentId(id);
        // 删除文档记录
        documentMapper.deleteById(id);
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 验证文件类型
        String fileType = determineFileType(originalFilename);
        if (!"txt".equals(fileType) && !"md".equals(fileType)) {
            throw new IllegalArgumentException("只支持TXT和Markdown格式的文件");
        }

        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }
    }

    /**
     * 确定文件类型
     */
    private String determineFileType(String filename) {
        if (filename == null) {
            return "txt";
        }
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".md") || lowerFilename.endsWith(".markdown")) {
            return "md";
        }
        return "txt";
    }

    /**
     * 异步处理文档（分块和向量化）
     */
    @Async
    public void processDocumentAsync(Long documentId) {
        try {
            log.info("开始处理文档: {}", documentId);

            // 获取文档
            KnowledgeDocument document = documentMapper.findById(documentId);
            if (document == null) {
                log.error("文档不存在: {}", documentId);
                return;
            }

            // 1. 使用 TextSplitter 进行文本分块
            List<String> chunks = textSplitter.split(document.getContent());
            int chunkCount = chunks.size();
            log.info("文档分块完成: docId={}, chunks={}", documentId, chunkCount);

            // 2. 向量化并存储到 PostgreSQL
            boolean vectorized = false;
            log.info("向量化服务是否启用: {}", vectorStoreService.isEnabled());
            if (!chunks.isEmpty() && vectorStoreService.isEnabled()) {
                // 先删除旧的向量（如果有）
                vectorStoreService.deleteByDocumentId(document.getId());
                // 写入新向量
                int saved = vectorStoreService.saveChunks(
                        document.getKnowledgeBaseId(),
                        document.getId(),
                        chunks
                );
                vectorized = saved > 0;
                log.info("向量化完成: docId={}, saved={}", documentId, saved);
            }

            // 3. 更新文档状态
            document.setStatus("processed");
            document.setChunkCount(chunkCount);
            document.setVectorized(vectorized);
            document.setUpdatedAt(LocalDateTime.now());

            documentMapper.update(document);

            log.info("文档处理完成: docId={}, chunks={}, vectorized={}", documentId, chunkCount, vectorized);
        } catch (Exception e) {
            log.error("处理文档失败: {}", documentId, e);
            // 更新文档状态为失败
            try {
                KnowledgeDocument document = documentMapper.findById(documentId);
                if (document != null) {
                    document.setStatus("failed");
                    document.setErrorMessage(e.getMessage());
                    document.setUpdatedAt(LocalDateTime.now());
                    documentMapper.update(document);
                }
            } catch (Exception ex) {
                log.error("更新文档状态失败", ex);
            }
        }
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getUserId(),
                document.getFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getStatus(),
                document.getChunkCount(),
                document.getVectorized(),
                document.getErrorMessage(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
