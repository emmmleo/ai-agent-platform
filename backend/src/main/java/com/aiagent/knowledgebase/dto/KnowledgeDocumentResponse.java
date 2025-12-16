package com.aiagent.knowledgebase.dto;

import java.time.LocalDateTime;

/**
 * 知识库文档响应
 */
public class KnowledgeDocumentResponse {

    private Long id;
    private Long knowledgeBaseId;
    private Long userId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String status; // processing/processed/failed
    private Integer chunkCount;
    private Boolean vectorized;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KnowledgeDocumentResponse() {
    }

    public KnowledgeDocumentResponse(Long id, Long knowledgeBaseId, Long userId,
                                    String fileName, String fileType, Long fileSize,
                                    String status, Integer chunkCount, Boolean vectorized,
                                    String errorMessage, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.userId = userId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.status = status;
        this.chunkCount = chunkCount;
        this.vectorized = vectorized;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Boolean getVectorized() {
        return vectorized;
    }

    public void setVectorized(Boolean vectorized) {
        this.vectorized = vectorized;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

