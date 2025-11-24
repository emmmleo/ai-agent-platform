package Maven.Project.auth.controller;

import Maven.Project.auth.dto.UserManagementVO;
import Maven.Project.auth.service.UserService;
import Maven.Project.auth.util.JwtUtil;
import Maven.Project.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class UserManagementController {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    
    public UserManagementController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * 获取所有普通用户列表
     */
    @GetMapping("/users")
    public ApiResponse<List<UserManagementVO>> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        // 从 token 中获取用户 ID 和角色（拦截器已验证为管理员）
        String token = authHeader.replace("Bearer ", "");
        
        List<UserManagementVO> users = userService.getAllNonAdminUsers();
        return ApiResponse.ok(users);
    }
    
    /**
     * 删除指定用户
     */
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        // 从 token 中获取操作者 ID
        String token = authHeader.replace("Bearer ", "");
        Long operatorId = jwtUtil.getUserIdFromToken(token);
        
        userService.deleteUser(id, operatorId);
        return ApiResponse.ok();
    }
}

