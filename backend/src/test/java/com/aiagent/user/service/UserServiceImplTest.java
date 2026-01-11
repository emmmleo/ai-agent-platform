package com.aiagent.user.service;

import com.aiagent.user.dto.UserProfileUpdateRequest;
import com.aiagent.user.entity.User;
import com.aiagent.user.mapper.UserMapper;
import com.aiagent.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceImplTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    void setup() {
        userMapper = Mockito.mock(UserMapper.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        userService = new UserServiceImpl(userMapper, passwordEncoder);
    }

    @Test
    void updateProfile_updatesFields() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("alice");

        Mockito.when(userMapper.findByUsername("alice")).thenReturn(existing);
        Mockito.when(userMapper.updateProfile(
                1L,
                "某大学",
                "+8613800138000",
                "alice@example.com",
                "hello",
                "https://example.com/avatar.png",
                "FEMALE",
                java.time.LocalDate.of(2000, 1, 1)
        ))
                .thenReturn(1);

        User after = new User();
        after.setId(1L);
        after.setUsername("alice");
        after.setSchool("某大学");
        after.setPhone("+8613800138000");
        after.setEmail("alice@example.com");
        after.setBio("hello");
        after.setAvatarUrl("https://example.com/avatar.png");
        after.setGender("FEMALE");
        after.setBirthday(java.time.LocalDate.of(2000, 1, 1));

        Mockito.when(userMapper.findById(1L)).thenReturn(after);

        UserProfileUpdateRequest req = new UserProfileUpdateRequest();
        req.setSchool("某大学");
        req.setPhone("+8613800138000");
        req.setEmail("alice@example.com");
        req.setBio("hello");
        req.setAvatarUrl("https://example.com/avatar.png");
        req.setGender("FEMALE");
        req.setBirthday(java.time.LocalDate.of(2000, 1, 1));

        User result = userService.updateProfile("alice", req);

        Assertions.assertEquals("某大学", result.getSchool());
        Assertions.assertEquals("+8613800138000", result.getPhone());
        Assertions.assertEquals("alice@example.com", result.getEmail());
        Assertions.assertEquals("hello", result.getBio());
        Assertions.assertEquals("https://example.com/avatar.png", result.getAvatarUrl());
        Assertions.assertEquals("FEMALE", result.getGender());
        Assertions.assertEquals(java.time.LocalDate.of(2000, 1, 1), result.getBirthday());
    }
}
