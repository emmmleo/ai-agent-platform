package Maven.Project.auth.service;

import Maven.Project.auth.dto.AuthResponse;
import Maven.Project.auth.dto.LoginRequest;
import Maven.Project.auth.dto.RegisterRequest;
import Maven.Project.auth.dto.UpdateUserProfileRequest;
import Maven.Project.auth.dto.UserInfo;
import Maven.Project.auth.dto.UserManagementVO;
import Maven.Project.auth.entity.UserEntity;
import Maven.Project.auth.mapper.UserMapper;
import Maven.Project.auth.util.JwtUtil;
import Maven.Project.common.BusinessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public UserService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    
    /**
     * 用户注册
     */
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        UserEntity existingUser = userMapper.findByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        
        // 验证用户名和密码
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException(400, "密码长度至少为 6 位");
        }
        
        // 创建新用户
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("user");  // 新用户默认为普通用户
        user.setCreatedAt(LocalDateTime.now());
        
        userMapper.insert(user);
        
        // 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .email(user.getEmail())
                .organization(user.getOrganization())
                .phone(user.getPhone())
                .bio(user.getBio())
                .build();
    }
    
    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        // 查找用户
        UserEntity user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .email(user.getEmail())
                .organization(user.getOrganization())
                .phone(user.getPhone())
                .bio(user.getBio())
                .build();
    }
    
    /**
     * 根据 token 获取用户信息
     */
    public UserInfo getUserInfo(String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在或已被删除");
        }
        return buildUserInfo(user, null);
    }

    public UserInfo getUserProfile(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在或已被删除");
        }
        return buildUserInfo(user, null);
    }

    public UserInfo updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在或已被删除");
        }

        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }

        String normalizedUsername = request.getUsername().trim();
        if (!normalizedUsername.equals(user.getUsername())) {
            int count = userMapper.countByUsernameExcludingId(normalizedUsername, userId);
            if (count > 0) {
                throw new BusinessException(400, "用户名已存在");
            }
        }

        user.setUsername(normalizedUsername);
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setOrganization(normalizeNullable(request.getOrganization()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setBio(normalizeNullable(request.getBio()));

        userMapper.updateProfile(user);

        UserEntity updated = userMapper.findById(userId);
        String newToken = jwtUtil.generateToken(updated.getId(), updated.getUsername(), updated.getRole());
        return buildUserInfo(updated, newToken);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserInfo buildUserInfo(UserEntity user, String token) {
        return UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .email(user.getEmail())
                .organization(user.getOrganization())
                .phone(user.getPhone())
                .bio(user.getBio())
                .token(token)
                .build();
    }
    
    /**
     * 获取所有非管理员用户
     */
    public List<UserManagementVO> getAllNonAdminUsers() {
        List<UserEntity> users = userMapper.findAllNonAdminUsers();
        return users.stream()
                .map(user -> UserManagementVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 删除用户（仅管理员可操作）
     */
    public void deleteUser(Long userId, Long operatorId) {
        // 验证操作者是管理员
        UserEntity operator = userMapper.findById(operatorId);
        if (operator == null || !"admin".equals(operator.getRole())) {
            throw new BusinessException(403, "无权限执行此操作");
        }
        
        // 验证被删除的用户存在
        UserEntity targetUser = userMapper.findById(userId);
        if (targetUser == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 不能删除管理员
        if ("admin".equals(targetUser.getRole())) {
            throw new BusinessException(400, "不能删除管理员用户");
        }
        
        // 执行删除
        userMapper.deleteById(userId);
    }
}

