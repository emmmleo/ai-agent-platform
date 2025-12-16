package com.aiagent.agent.mapper;

import com.aiagent.agent.entity.Agent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMapper {

    List<Agent> findByUserId(@Param("userId") Long userId);

    Agent findById(@Param("id") Long id);

    Agent findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int insert(Agent agent);

    int update(Agent agent);

    int deleteById(@Param("id") Long id);
}

