package com.aiagent.knowledgebase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建知识库请求
 */
public class CreateKnowledgeBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(min = 1, max = 100, message = "知识库名称长度需1-100个字符")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

