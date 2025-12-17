package com.aiagent.workflow.service.executor.impl;

import com.aiagent.agent.client.DeepSeekClient;
import com.aiagent.workflow.entity.WorkflowNode;
import com.aiagent.workflow.service.executor.NodeExecutor;
import com.aiagent.workflow.service.executor.WorkflowExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class IntentRecognitionNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognitionNodeExecutor.class);
    private static final String NODE_TYPE = "intent_recognition";
    private static final String DEFAULT_INTENT = "default";

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Override
    public String getSupportedNodeType() {
        return NODE_TYPE;
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(WorkflowNode node, WorkflowExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new HashMap<>();
            try {
                // 读取节点配置
                Map<String, Object> data = node.getData();
                if (data == null) {
                    data = Collections.emptyMap();
                }

                // 处理text参数，支持变量替换
                String text = (String) data.get("text");
                if (text == null) {
                    throw new IllegalArgumentException("Intent recognition node requires 'text' parameter");
                }
                String processedText = context.replaceVariables(text);

                // 处理intents参数
                List<Map<String, Object>> intents = getIntents(data);
                if (intents.isEmpty()) {
                    throw new IllegalArgumentException("Intent recognition node requires non-empty 'intents' parameter");
                }

                // 处理model参数（可选）
                String model = (String) data.get("model");
                Map<String, Object> modelConfig = new HashMap<>();
                if (model != null) {
                    modelConfig.put("model", model);
                }

                // 构建Prompt
                String prompt = buildPrompt(processedText, intents);

                // 调用大模型
                DeepSeekClient.Message response = deepSeekClient.chat(null, prompt, null, modelConfig);
                
                // 解析结果
                String rawIntent = response.getContent();
                String cleanIntent = cleanIntentResult(rawIntent);

                // 验证意图是否在候选列表中
                String finalIntent = validateIntent(cleanIntent, intents);

                // 设置结果
                result.put("intent", finalIntent);
                result.put("match", cleanIntent);

                log.info("Intent recognition completed for node {}. Text: {}, Recognized intent: {}", 
                        node.getId(), processedText, finalIntent);

            } catch (Exception e) {
                log.error("Error in intent recognition node {}", node.getId(), e);
                // 设置默认意图
                result.put("intent", DEFAULT_INTENT);
                result.put("error", e.getMessage());
            }

            return result;
        });
    }

    private List<Map<String, Object>> getIntents(Map<String, Object> data) {
        List<Map<String, Object>> intents = new ArrayList<>();
        Object intentsObj = data.get("intents");
        
        if (intentsObj instanceof List<?>) {
            for (Object item : (List<?>) intentsObj) {
                if (item instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> intentMap = (Map<String, Object>) item;
                    intents.add(intentMap);
                }
            }
        }
        
        return intents;
    }

    private String buildPrompt(String text, List<Map<String, Object>> intents) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个意图分类器。请分析文本：'");
        promptBuilder.append(text);
        promptBuilder.append("'。从以下候选意图中选择最匹配的一个：\n");
        
        for (Map<String, Object> intent : intents) {
            String name = (String) intent.get("name");
            String description = (String) intent.get("description");
            promptBuilder.append("- ").append(name);
            if (description != null) {
                promptBuilder.append("：").append(description);
            }
            promptBuilder.append("\n");
        }
        
        promptBuilder.append("请仅返回意图的name，不要包含任何其他解释或标点。");
        return promptBuilder.toString();
    }

    private String cleanIntentResult(String rawResult) {
        if (rawResult == null) {
            return "";
        }
        // 去除多余空格、换行和标点符号
        return rawResult.trim()
                .replaceAll("[\n\r]+", "")
                .replaceAll("^['\"`]+", "")
                .replaceAll("['\"`]+$", "")
                .replaceAll("^[^a-zA-Z0-9_]+|[^a-zA-Z0-9_]+$", "");
    }

    private String validateIntent(String recognizedIntent, List<Map<String, Object>> intents) {
        if (recognizedIntent == null || recognizedIntent.isEmpty()) {
            return DEFAULT_INTENT;
        }
        
        // 检查识别出的意图是否在候选列表中
        for (Map<String, Object> intent : intents) {
            String name = (String) intent.get("name");
            if (recognizedIntent.equals(name)) {
                return name;
            }
        }
        
        // 如果不在候选列表中，尝试模糊匹配
        for (Map<String, Object> intent : intents) {
            String name = (String) intent.get("name");
            if (recognizedIntent.contains(name)) {
                return name;
            }
        }
        
        // 如果仍然没有匹配，返回默认意图
        log.warn("Recognized intent '{}' not found in candidate list. Returning default intent.", recognizedIntent);
        return DEFAULT_INTENT;
    }
}