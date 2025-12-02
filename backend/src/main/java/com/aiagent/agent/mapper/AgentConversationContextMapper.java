package com.aiagent.agent.mapper;

import com.aiagent.agent.entity.AgentConversationContext;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AgentConversationContextMapper {

    java.util.List<AgentConversationContext> findSessions(@Param("agentId") Long agentId,
                                                          @Param("userId") Long userId);

    AgentConversationContext findById(@Param("id") Long id);

    AgentConversationContext findByIdAndOwner(@Param("id") Long id,
                                              @Param("agentId") Long agentId,
                                              @Param("userId") Long userId);

    int insert(AgentConversationContext context);

    int updateMessagesById(@Param("id") Long id,
                           @Param("messages") String messages,
                           @Param("updatedAt") LocalDateTime updatedAt);

    int updateTitle(@Param("id") Long id,
                    @Param("title") String title,
                    @Param("updatedAt") LocalDateTime updatedAt);

    int deleteById(@Param("id") Long id);
}
