package com.aiagent.user.mapper;

import com.aiagent.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);

    List<User> findAll();

    int insertUser(User user);

    int updateLastLoginAt(@Param("id") Long id);

    int deleteById(@Param("id") Long id);
}

