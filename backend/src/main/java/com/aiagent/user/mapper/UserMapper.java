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

    int updateUser(User user);

    int updateProfile(@Param("id") Long id,
                      @Param("school") String school,
                      @Param("phone") String phone,
                      @Param("email") String email,
                      @Param("bio") String bio,
                      @Param("avatarUrl") String avatarUrl,
                      @Param("gender") String gender,
                      @Param("birthday") java.time.LocalDate birthday);

    int deleteById(@Param("id") Long id);
}
