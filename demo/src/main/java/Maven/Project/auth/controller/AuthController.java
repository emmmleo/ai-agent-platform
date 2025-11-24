package Maven.Project.auth.controller;

import Maven.Project.auth.dto.AuthResponse;
import Maven.Project.auth.dto.LoginRequest;
import Maven.Project.auth.dto.RegisterRequest;
import Maven.Project.auth.dto.UpdateUserProfileRequest;
import Maven.Project.auth.dto.UserInfo;
import Maven.Project.auth.service.UserService;
import Maven.Project.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ApiResponse.ok(response);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ApiResponse.ok(response);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserInfo> getCurrentUser(HttpServletRequest request,
                                                @RequestHeader("Authorization") String authHeader) {
        Long userId = (Long) request.getAttribute("userId");
        UserInfo userInfo;
        if (userId != null) {
            userInfo = userService.getUserProfile(userId);
        } else {
            String token = authHeader.replace("Bearer ", "");
            userInfo = userService.getUserInfo(token);
        }
        return ApiResponse.ok(userInfo);
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/me")
    public ApiResponse<UserInfo> updateCurrentUser(HttpServletRequest request,
                                                   @RequestBody UpdateUserProfileRequest updateRequest,
                                                   @RequestHeader("Authorization") String authHeader) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            String token = authHeader.replace("Bearer ", "");
            userId = userService.getUserInfo(token).getUserId();
        }
        UserInfo userInfo = userService.updateUserProfile(userId, updateRequest);
        return ApiResponse.ok(userInfo);
    }
}

