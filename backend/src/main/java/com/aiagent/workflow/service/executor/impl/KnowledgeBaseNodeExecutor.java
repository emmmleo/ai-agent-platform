package com.aiagent.workflow.service.executor.impl;

import com.aiagent.knowledgebase.service.KnowledgeRetrievalService;
import com.aiagent.workflow.entity.WorkflowNode;
import com.aiagent.workflow.service.executor.NodeExecutor;
import com.aiagent.workflow.service.executor.WorkflowExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 知识库检索节点执行器
 * 实现了NodeExecutor接口，用于处理知识库检索节点
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
                
                // 从节点数据中获取配置参数
                Map<String, Object> data = node.getData();
                if (data == null) {
                    throw new IllegalArgumentException("知识库检索节点配置数据不能为空");
                }

                // 获取查询文本并进行变量替换
                String query = (String) data.get("query");
                if (query == null || query.isEmpty()) {
                    throw new IllegalArgumentException("查询文本不能为空");
                }
                query = context.replaceVariables(query);
                logger.info("处理后的查询文本: {}", query);

                // 获取返回数量
                Integer topK = getIntegerParam(data, "topK", 3);
                logger.info("返回数量: {}", topK);

                // 获取最小相似度分数
                Double minScore = getDoubleParam(data, "minScore", 0.0);
                logger.info("最小相似度分数: {}", minScore);

                // 获取知识库ID列表
                List<Long> knowledgeBaseIds = getKnowledgeBaseIds(data);
                if (knowledgeBaseIds.isEmpty()) {
                    throw new IllegalArgumentException("知识库ID列表不能为空");
                }
                logger.info("知识库ID列表: {}", knowledgeBaseIds);

                // 执行知识库检索
                List<KnowledgeRetrievalService.RetrievalResultItem> results = knowledgeRetrievalService.retrieve(
                        knowledgeBaseIds, query, topK, minScore
                );

                // 构建响应结果
                Map<String, Object> result = Collections.singletonMap("results", results);
                logger.info("知识库检索完成，返回结果数量: {}", results.size());
                
                return result;

            } catch (Exception e) {
                logger.error("知识库检索节点执行失败: {}", e.getMessage(), e);
                throw new RuntimeException("知识库检索节点执行失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 获取整数类型参数
     */
    private Integer getIntegerParam(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("参数{}的值{}不是有效整数，使用默认值{}", key, value, defaultValue);
                return defaultValue;
            }
        }
        logger.warn("参数{}的值{}不是有效整数，使用默认值{}", key, value, defaultValue);
        return defaultValue;
    }

    /**
     * 获取浮点数类型参数
     */
    private Double getDoubleParam(Map<String, Object> data, String key, Double defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                logger.warn("参数{}的值{}不是有效浮点数，使用默认值{}", key, value, defaultValue);
                return defaultValue;
            }
        }
        logger.warn("参数{}的值{}不是有效浮点数，使用默认值{}", key, value, defaultValue);
        return defaultValue;
    }

    /**
     * 获取知识库ID列表
     */
    private List<Long> getKnowledgeBaseIds(Map<String, Object> data) {
        Object value = data.get("knowledgeBaseIds");
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .map(item -> {
                        if (item instanceof Long) {
                            return (Long) item;
                        } else if (item instanceof Integer) {
                            return ((Integer) item).longValue();
                        } else if (item instanceof String) {
                            try {
                                return Long.parseLong((String) item);
                            } catch (NumberFormatException e) {
                                logger.warn("知识库ID {} 不是有效数字，忽略", item);
                                return null;
                            }
                        } else {
                            logger.warn("知识库ID {} 不是有效类型，忽略", item);
                            return null;
                        }
                    })
                    .filter(id -> id != null)
                    .collect(java.util.stream.Collectors.toList());
        }
        logger.warn("知识库ID参数 {} 不是有效列表，使用空列表", value);
        return Collections.emptyList();
    }
}