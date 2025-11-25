package com.aiagent.knowledgebase.entity;

import java.time.LocalDateTime;

/**
 * 知识库文档实体类
 */
public class KnowledgeDocument {

    private Long id;
    private Long knowledgeBaseId; // 所属知识库ID
    private Long userId; // 上传者ID
    private String fileName; // 文件名
    private String fileType; // 文件类型：txt/md
    private Long fileSize; // 文件大小（字节）
    private String content; // 文档内容
    private String status; // 处理状态：processing/processed/failed
    private Integer chunkCount; // 分块数量
    private Boolean vectorized; // 是否已向量化
    private String errorMessage; // 错误信息
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KnowledgeDocument() {
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

