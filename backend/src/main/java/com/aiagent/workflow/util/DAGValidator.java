package com.aiagent.workflow.util;

import com.aiagent.workflow.dto.CreateWorkflowRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * DAG（有向无环图）验证器
 */
public class DAGValidator {

    private static final Logger log = LoggerFactory.getLogger(DAGValidator.class);

    /**
     * 验证工作流定义是否为有效的DAG
     */
    public static void validate(CreateWorkflowRequest.WorkflowDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("工作流定义不能为空");
        }

        List<CreateWorkflowRequest.Node> nodes = definition.getNodes();
        List<CreateWorkflowRequest.Edge> edges = definition.getEdges();

        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("工作流必须至少包含一个节点");
        }

        if (edges == null) {
            edges = new ArrayList<>();
        }

        // 1. 检查节点ID唯一性
        Set<String> nodeIds = new HashSet<>();
        for (CreateWorkflowRequest.Node node : nodes) {
            if (node.getId() == null || node.getId().trim().isEmpty()) {
                throw new IllegalArgumentException("节点ID不能为空");
            }
            if (nodeIds.contains(node.getId())) {
                throw new IllegalArgumentException("节点ID重复: " + node.getId());
            }
            nodeIds.add(node.getId());
        }

        // 2. 检查边的节点引用
        for (CreateWorkflowRequest.Edge edge : edges) {
            if (edge.getSource() == null || edge.getTarget() == null) {
                throw new IllegalArgumentException("边的源节点和目标节点不能为空");
            }
            if (!nodeIds.contains(edge.getSource())) {
                throw new IllegalArgumentException("边的源节点不存在: " + edge.getSource());
            }
            if (!nodeIds.contains(edge.getTarget())) {
                throw new IllegalArgumentException("边的目标节点不存在: " + edge.getTarget());
            }
            if (edge.getSource().equals(edge.getTarget())) {
                throw new IllegalArgumentException("节点不能连接到自身: " + edge.getSource());
            }
        }

        // 3. 检查是否有起始节点和结束节点
        Set<String> sourceNodes = new HashSet<>();
        Set<String> targetNodes = new HashSet<>();
        for (CreateWorkflowRequest.Edge edge : edges) {
            sourceNodes.add(edge.getSource());
            targetNodes.add(edge.getTarget());
        }

        // 找到起始节点（没有入边的节点）
        Set<String> startNodes = new HashSet<>(nodeIds);
        startNodes.removeAll(targetNodes);

        // 找到结束节点（没有出边的节点）
        Set<String> endNodes = new HashSet<>(nodeIds);
        endNodes.removeAll(sourceNodes);

        if (startNodes.isEmpty()) {
            throw new IllegalArgumentException("工作流必须至少包含一个起始节点（没有入边的节点）");
        }

        if (endNodes.isEmpty()) {
            throw new IllegalArgumentException("工作流必须至少包含一个结束节点（没有出边的节点）");
        }

        // 4. 检查是否有环（使用拓扑排序）
        if (hasCycle(nodes, edges)) {
            throw new IllegalArgumentException("工作流不能包含环（循环依赖）");
        }

        // 5. 检查所有节点是否可达（从起始节点开始）
        if (!isAllNodesReachable(nodes, edges, startNodes)) {
            throw new IllegalArgumentException("工作流中存在不可达的节点");
        }

        log.info("工作流DAG验证通过，节点数: {}, 边数: {}, 起始节点: {}, 结束节点: {}",
                nodes.size(), edges.size(), startNodes.size(), endNodes.size());
    }

    /**
     * 检查是否有环（使用DFS）
     */
    private static boolean hasCycle(List<CreateWorkflowRequest.Node> nodes,
                                    List<CreateWorkflowRequest.Edge> edges) {
        // 构建邻接表
        Map<String, List<String>> graph = new HashMap<>();
        for (CreateWorkflowRequest.Node node : nodes) {
            graph.put(node.getId(), new ArrayList<>());
        }
        for (CreateWorkflowRequest.Edge edge : edges) {
            graph.get(edge.getSource()).add(edge.getTarget());
        }

        // 使用DFS检测环
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (CreateWorkflowRequest.Node node : nodes) {
            if (hasCycleDFS(node.getId(), graph, visited, recursionStack)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasCycleDFS(String nodeId, Map<String, List<String>> graph,
                                      Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true; // 发现环
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        for (String neighbor : graph.get(nodeId)) {
            if (hasCycleDFS(neighbor, graph, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    /**
     * 检查所有节点是否可达
     */
    private static boolean isAllNodesReachable(List<CreateWorkflowRequest.Node> nodes,
                                               List<CreateWorkflowRequest.Edge> edges,
                                               Set<String> startNodes) {
        // 构建邻接表（反向）
        Map<String, List<String>> reverseGraph = new HashMap<>();
        for (CreateWorkflowRequest.Node node : nodes) {
            reverseGraph.put(node.getId(), new ArrayList<>());
        }
        for (CreateWorkflowRequest.Edge edge : edges) {
            reverseGraph.get(edge.getTarget()).add(edge.getSource());
        }

        // 从所有起始节点开始BFS
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>(startNodes);
        reachable.addAll(startNodes);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<String> neighbors = reverseGraph.get(current);
            if (neighbors != null) {
                for (String neighbor : neighbors) {
                    if (!reachable.contains(neighbor)) {
                        reachable.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
        }

        // 检查所有节点是否可达
        return reachable.size() == nodes.size();
    }
}

