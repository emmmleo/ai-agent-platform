package com.aiagent.user.service;

import com.aiagent.user.dto.RegisterRequest;
import com.aiagent.user.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {

    User register(RegisterRequest request);

    User findByUsername(String username);

    User findById(Long id);

    List<User> findAll();

    void markLogin(Long id);

    void deleteByUsername(String username);

    com.aiagent.user.entity.User updateProfile(String username, com.aiagent.user.dto.UserProfileUpdateRequest request);
}
