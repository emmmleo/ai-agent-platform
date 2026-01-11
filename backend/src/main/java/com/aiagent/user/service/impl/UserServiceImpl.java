package com.aiagent.user.service.impl;

import com.aiagent.user.dto.RegisterRequest;
import com.aiagent.user.dto.UserProfileUpdateRequest;
import com.aiagent.user.entity.User;
import com.aiagent.user.mapper.UserMapper;
import com.aiagent.user.service.UserService;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        User existing = userMapper.findByUsername(request.getUsername());
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(DEFAULT_ROLE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userMapper.insertUser(user);

        return user;
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public void markLogin(Long id) {
        userMapper.updateLastLoginAt(id);
    }

    @Override
    public void deleteByUsername(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        userMapper.deleteById(user.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = Optional.ofNullable(userMapper.findByUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                AuthorityUtils.createAuthorityList(user.getRole())
        );
    }

    @Override
    @Transactional
    public User updateProfile(String username, UserProfileUpdateRequest request) {
        User user = Optional.ofNullable(userMapper.findByUsername(username))
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        String school = normalize(request.getSchool());
        String phone = normalizePhone(request.getPhone());
        String email = normalize(request.getEmail());
        String bio = normalize(request.getBio());
        String avatarUrl = normalize(request.getAvatarUrl());
        String gender = normalizeGender(request.getGender());
        java.time.LocalDate birthday = request.getBirthday();

        if (birthday != null && birthday.isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("生日不能晚于今天");
        }

        userMapper.updateProfile(user.getId(), school, phone, email, bio, avatarUrl, gender, birthday);

        return userMapper.findById(user.getId());
    }

    private String normalizeGender(String input) {
        String v = normalize(input);
        if (v == null) {
            return null;
        }
        String upper = v.toUpperCase();
        return switch (upper) {
            case "MALE", "FEMALE", "UNKNOWN" -> upper;
            case "男" -> "MALE";
            case "女" -> "FEMALE";
            case "保密" -> "UNKNOWN";
            default -> throw new IllegalArgumentException("性别仅支持 MALE/FEMALE/UNKNOWN");
        };
    }

    private String normalize(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePhone(String s) {
        if (s == null) return null;
        String cleaned = s.replaceAll("[\\s-]", "");
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) return null;
        return cleaned;
    }
}
