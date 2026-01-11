package com.aiagent.workflow.service.executor.impl;

import com.aiagent.knowledgebase.service.KnowledgeRetrievalService;
import com.aiagent.workflow.entity.WorkflowNode;
import com.aiagent.workflow.service.executor.NodeExecutor;
import com.aiagent.workflow.service.executor.WorkflowExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 知识库检索节点执行器
 * 实现了NodeExecutor接口，用于处理知识库检索节点
 * 迁移自 CodeHubot (支持 combined_content 等富输出)
 */
@Component
public class KnowledgeBaseNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseNodeExecutor.class);

    @Autowired
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Override
    public String getSupportedNodeType() {
        return "knowledge_retrieval";
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(WorkflowNode node, WorkflowExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("开始执行知识库检索节点: {}", node.getName());
                
                // 1. 获取配置参数
                Map<String, Object> data = node.getData();
                if (data == null) throw new IllegalArgumentException("知识库检索节点配置数据不能为空");

                String queryRaw = (String) data.get("query");
                if (queryRaw == null || queryRaw.isEmpty()) throw new IllegalArgumentException("查询文本不能为空");
                
                // 变量替换
                String query = context.replaceVariables(queryRaw);
                logger.info("处理后的查询文本: {}", query);

                Integer topK = getIntegerParam(data, "topK", 3);
                Double minScore = getDoubleParam(data, "minScore", 0.6); // 默认阈值0.6

                List<Long> knowledgeBaseIds = getKnowledgeBaseIds(data);
                if (knowledgeBaseIds.isEmpty()) {
                    // 如果未指定KB，尝试从变量或旧格式获取，或者抛出异常。CodeHubot 必须指定 UUID。
                    // 这里我们容忍空列表，但返回空结果
                    logger.warn("未指定知识库ID");
                }

                // 2. 执行检索
                List<KnowledgeRetrievalService.RetrievalResultItem> items = Collections.emptyList();
                if (!knowledgeBaseIds.isEmpty()) {
                    items = knowledgeRetrievalService.retrieve(knowledgeBaseIds, query, topK, minScore);
                }

                // 3. 构建富输出结果 (对齐 CodeHubot)
                List<Map<String, Object>> results = items.stream().map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("content", item.content());
                    map.put("score", item.score());
                    map.put("similarity", item.score()); // Alias
                    map.put("metadata", item.metadata());
                    // 提取 document_title 供引用
                    if (item.metadata() != null) {
                        map.put("document_title", item.metadata().getOrDefault("title", "未知文档"));
                        map.put("document_id", item.metadata().get("documentId"));
                        map.put("chunk_id", item.metadata().get("chunkId"));
                    }
                    return map;
                }).collect(Collectors.toList());

                Map<String, Object> output = new HashMap<>();
                output.put("results", results);
                output.put("total", results.size());
                output.put("query", query);
                
                if (!results.isEmpty()) {
                    Map<String, Object> topResult = results.get(0);
                    output.put("top_result", topResult);
                    output.put("top_content", topResult.get("content"));
                    output.put("top_similarity", topResult.get("score"));

                    // 构造 combined_content: 【来源：Title】\nContent
                    String combinedContent = results.stream()
                        .map(r -> {
                            String title = (String) r.getOrDefault("document_title", "未知来源");
                            String content = (String) r.get("content");
                            return String.format("【来源：%s】\n%s", title, content);
                        })
                        .collect(Collectors.joining("\n\n---\n\n"));
                    output.put("combined_content", combinedContent);
                } else {
                    output.put("combined_content", ""); 
                    output.put("top_content", "");
                }

                logger.info("知识库检索完成，找到 {} 个结果", results.size());
                return output;

            } catch (Exception e) {
                logger.error("知识库检索节点执行失败", e);
                throw new RuntimeException("知识库检索节点执行失败: " + e.getMessage(), e);
            }
        });
    }

    private Integer getIntegerParam(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Double getDoubleParam(Map<String, Object> data, String key, Double defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private List<Long> getKnowledgeBaseIds(Map<String, Object> data) {
        Object value = data.get("knowledgeBaseIds");
        if (value == null) return Collections.emptyList();
        
        List<Long> ids = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                Long id = parseLong(item);
                if (id != null) ids.add(id);
            }
        } else {
            // 支持单个ID的情况
            Long id = parseLong(value);
            if (id != null) ids.add(id);
        }
        return ids;
    }

    private Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}