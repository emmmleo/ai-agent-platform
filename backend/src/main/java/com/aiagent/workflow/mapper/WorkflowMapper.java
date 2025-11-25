package com.aiagent.workflow.mapper;

import com.aiagent.workflow.entity.Workflow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkflowMapper {

    /**
     * 根据用户ID查询所有工作流
     */
    List<Workflow> findByUserId(@Param("userId") Long userId);

    /**
     * 根据ID查询工作流
     */
    Workflow findById(@Param("id") Long id);

    /**
     * 根据ID和用户ID查询工作流（确保权限）
     */
    Workflow findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 插入工作流
     */
    int insert(Workflow workflow);

    /**
     * 更新工作流
     */
    int update(Workflow workflow);

    /**
     * 删除工作流
     */
    int deleteById(@Param("id") Long id);
}

