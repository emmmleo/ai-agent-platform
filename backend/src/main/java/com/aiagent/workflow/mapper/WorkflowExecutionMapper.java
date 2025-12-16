package com.aiagent.workflow.mapper;

import com.aiagent.workflow.entity.WorkflowExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkflowExecutionMapper {

    /**
     * 根据工作流ID查询执行记录
     */
    List<WorkflowExecution> findByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * 根据用户ID查询执行记录
     */
    List<WorkflowExecution> findByUserId(@Param("userId") Long userId);

    /**
     * 根据ID查询执行记录
     */
    WorkflowExecution findById(@Param("id") Long id);

    /**
     * 插入执行记录
     */
    int insert(WorkflowExecution execution);

    /**
     * 更新执行记录
     */
    int update(WorkflowExecution execution);
}

