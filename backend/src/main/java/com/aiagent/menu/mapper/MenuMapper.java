package com.aiagent.menu.mapper;

import com.aiagent.menu.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MenuMapper {

    List<Menu> findAll();

    int insert(Menu menu);

    long count();
}

