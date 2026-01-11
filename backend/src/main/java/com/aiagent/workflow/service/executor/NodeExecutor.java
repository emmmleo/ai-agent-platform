package com.aiagent.workflow.service.executor;

import com.aiagent.workflow.entity.WorkflowNode;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 节点执行器接口
 * 定义了执行不同类型工作流节点的标准接口
 */
public interface NodeExecutor {
    /**
     * 获取支持的节点类型
     * @return 节点类型字符串
     */
    String getSupportedNodeType();

    /**
     * 执行节点
     * @param node 节点信息
     * @param context 执行上下文
     * @return 节点执行结果的CompletableFuture
     */
    CompletableFuture<Map<String, Object>> execute(WorkflowNode node, WorkflowExecutionContext context);
}