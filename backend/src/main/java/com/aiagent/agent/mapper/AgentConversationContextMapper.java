package com.aiagent.agent.mapper;

import com.aiagent.agent.entity.AgentConversationContext;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AgentConversationContextMapper {

    AgentConversationContext findByAgentAndUser(@Param("agentId") Long agentId,
                                                @Param("userId") Long userId);

    int insert(AgentConversationContext context);

    int updateMessages(@Param("agentId") Long agentId,
                       @Param("userId") Long userId,
                       @Param("messages") String messages,
                       @Param("updatedAt") LocalDateTime updatedAt);
}
