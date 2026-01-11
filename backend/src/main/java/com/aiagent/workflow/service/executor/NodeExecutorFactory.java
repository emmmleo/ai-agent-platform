package com.aiagent.workflow.service.executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点执行器工厂
 * 用于管理和获取不同类型的节点执行器
 */
@Component
public class NodeExecutorFactory {
    private final Map<String, NodeExecutor> executorMap = new ConcurrentHashMap<>();

    @Autowired
    public NodeExecutorFactory(ApplicationContext applicationContext) {
        // 自动发现所有实现了NodeExecutor接口的Bean
        Map<String, NodeExecutor> beans = applicationContext.getBeansOfType(NodeExecutor.class);
        beans.values().forEach(this::registerExecutor);
    }

    /**
     * 注册节点执行器
     */
    public void registerExecutor(NodeExecutor executor) {
        String nodeType = executor.getSupportedNodeType();
        executorMap.put(nodeType, executor);
    }

    /**
     * 根据节点类型获取执行器
     */
    public NodeExecutor getExecutor(String nodeType) {
        return executorMap.get(nodeType);
    }

    /**
     * 获取所有支持的节点类型
     */
    public List<String> getSupportedNodeTypes() {
        return List.copyOf(executorMap.keySet());
    }
}
