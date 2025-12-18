package com.aiagent.workflow.service.executor.impl;

import com.aiagent.agent.client.DeepSeekClient;
import com.aiagent.workflow.entity.WorkflowNode;
import com.aiagent.workflow.service.executor.NodeExecutor;
import com.aiagent.workflow.service.executor.WorkflowExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LLM节点执行器 (对应 CodeHubot 中的 LLMService)
 */
@Component
public class LLMNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(LLMNodeExecutor.class);

    private final ObjectProvider<DeepSeekClient> deepSeekClientProvider;

    public LLMNodeExecutor(ObjectProvider<DeepSeekClient> deepSeekClientProvider) {
        this.deepSeekClientProvider = deepSeekClientProvider;
    }

    @Override
    public String getSupportedNodeType() {
        return "llm"; // 同时也支持 "agent" 类型，可能需要注册两次或让工厂支持多类型
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(WorkflowNode node, WorkflowExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("开始执行LLM节点: {}", node.getName());
            
            try {
                // 1. 获取LLM客户端实例 (Prototype)
                DeepSeekClient llmClient = deepSeekClientProvider.getObject();

                // 2. 准备配置参数
                Map<String, Object> nodeData = node.getData() != null ? node.getData() : new HashMap<>();
                Map<String, Object> modelConfig = new HashMap<>(nodeData);

                // 3. 变量替换 (System Prompt, User Prompt)
                String systemPrompt = (String) modelConfig.get("system_prompt");
                if (systemPrompt != null) {
                    systemPrompt = context.replaceVariables(systemPrompt);
                    modelConfig.put("system_prompt", systemPrompt);
                }

                String userPrompt = (String) modelConfig.get("user_prompt");
                // 兼容 prompt 字段
                if (userPrompt == null && modelConfig.containsKey("prompt")) {
                    Object p = modelConfig.get("prompt");
                    if (p instanceof String) userPrompt = (String) p;
                }
                
                if (userPrompt != null) {
                    userPrompt = context.replaceVariables(userPrompt);
                } else {
                    throw new IllegalArgumentException("LLM节点缺少 'user_prompt' 或 'prompt' 参数");
                }

                logger.info("LLM Prompt: {}", userPrompt);

                // 4. 设置System Prompt
                if (StringUtils.hasText(systemPrompt)) {
                    llmClient.appendMessage("system", systemPrompt);
                }

                // 5. 调用大模型
                DeepSeekClient.Message responseMessage = llmClient.chat(
                        null, // context (optional)
                        userPrompt,
                        null, // tools (需扩展支持)
                        modelConfig
                );

                // 6. 构建输出
                Map<String, Object> result = new HashMap<>();
                if (responseMessage != null) {
                    result.put("output", responseMessage.getContent());
                    // Alias 'content' for compatibility with user expectations and templates
                    result.put("content", responseMessage.getContent()); 
                    result.put("message", responseMessage);
                    // 结构化数据供后续节点引用
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("response", responseMessage.getContent());
                    dataMap.put("role", responseMessage.getRole());
                    result.put("data", dataMap);
                } else {
                    result.put("output", "");
                    result.put("content", "");
                }
                
                logger.info("LLM节点执行完成: {}", node.getName());
                return result;

            } catch (Exception e) {
                logger.error("LLM节点执行失败: {}", e.getMessage(), e);
                throw new RuntimeException("LLM节点执行失败: " + e.getMessage(), e);
            }
        });
    }
}
