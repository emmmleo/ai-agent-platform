package com.aiagent.workflow.service.executor.impl;

import com.aiagent.workflow.entity.WorkflowNode;
import com.aiagent.workflow.service.executor.NodeExecutor;
import com.aiagent.workflow.service.executor.WorkflowExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP节点执行器
 * 实现了NodeExecutor接口，用于处理HTTP请求节点
 */
@Component
public class HttpNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(HttpNodeExecutor.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getSupportedNodeType() {
        return "http";
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(WorkflowNode node, WorkflowExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("开始执行HTTP节点: {}", node.getName());
                
                // 从节点数据中获取配置参数
                Map<String, Object> data = node.getData();
                if (data == null) {
                    throw new IllegalArgumentException("HTTP节点配置数据不能为空");
                }

                // 获取URL并进行变量替换
                String url = (String) data.get("url");
                if (url == null || url.isEmpty()) {
                    throw new IllegalArgumentException("HTTP请求URL不能为空");
                }
                url = context.replaceVariables(url);
                logger.info("处理后的URL: {}", url);

                // 获取请求方法并转换为HttpMethod
                String methodStr = (String) data.getOrDefault("method", "GET");
                HttpMethod method = HttpMethod.valueOf(methodStr.toUpperCase());

                // 获取请求头并进行变量替换
                Map<String, Object> headersMap = (Map<String, Object>) data.get("headers");
                HttpHeaders headers = new HttpHeaders();
                if (headersMap != null) {
                    for (Map.Entry<String, Object> entry : headersMap.entrySet()) {
                        String value = entry.getValue() != null ? entry.getValue().toString() : "";
                        headers.add(entry.getKey(), context.replaceVariables(value));
                    }
                }

                // 设置默认Content-Type
                if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }

                // 获取请求体并进行变量替换
                Object body = data.get("body");
                String processedBody = null;
                if (body != null) {
                    if (body instanceof String) {
                        processedBody = context.replaceVariables((String) body);
                    } else {
                        // 如果body是Map类型，转换为JSON字符串后进行变量替换
                        processedBody = objectMapper.writeValueAsString(body);
                        processedBody = context.replaceVariables(processedBody);
                    }
                }

                // 获取超时时间（毫秒）
                Integer timeout = (Integer) data.getOrDefault("timeout", 30000);

                // 创建请求实体
                HttpEntity<String> requestEntity = new HttpEntity<>(processedBody, headers);

                // 发送请求
                ResponseEntity<String> responseEntity;
                try {
                    responseEntity = restTemplate.exchange(url, method, requestEntity, String.class);
                } catch (Exception e) {
                    throw new RuntimeException("HTTP请求失败: " + e.getMessage(), e);
                }

                // 构建响应结果
                Map<String, Object> result = new HashMap<>();
                result.put("status", responseEntity.getStatusCodeValue());
                result.put("body", responseEntity.getBody());

                // 转换响应头为Map
                Map<String, List<String>> responseHeaders = responseEntity.getHeaders();
                Map<String, Object> headersResult = new HashMap<>();
                for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                    List<String> values = entry.getValue();
                    if (values.size() == 1) {
                        headersResult.put(entry.getKey(), values.get(0));
                    } else {
                        headersResult.put(entry.getKey(), values);
                    }
                }
                result.put("headers", headersResult);

                logger.info("HTTP节点执行完成: {}, 状态码: {}", node.getName(), responseEntity.getStatusCodeValue());
                return result;

            } catch (Exception e) {
                logger.error("HTTP节点执行失败: {}", e.getMessage(), e);
                throw new RuntimeException("HTTP节点执行失败: " + e.getMessage(), e);
            }
        });
    }
}