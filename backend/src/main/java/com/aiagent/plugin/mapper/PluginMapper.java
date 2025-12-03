package com.aiagent.plugin.mapper;

import com.aiagent.plugin.entity.Plugin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PluginMapper {

    /**
     * 根据用户ID查询所有插件
     */
    List<Plugin> findByUserId(@Param("userId") Long userId);

    /**
     * 查询所有启用的插件
     */
    List<Plugin> findEnabledPlugins();

    /**
     * 根据ID列表查询插件
     */
    List<Plugin> findByIds(@Param("ids") List<Long> ids);

    /**
     * 根据ID查询插件
     */
    Plugin findById(@Param("id") Long id);

    /**
     * 根据ID和用户ID查询插件（确保权限）
     */
    Plugin findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 插入插件
     */
    int insert(Plugin plugin);

    /**
     * 更新插件
     */
    int update(Plugin plugin);

    /**
     * 删除插件
     */
    int deleteById(@Param("id") Long id);
}

