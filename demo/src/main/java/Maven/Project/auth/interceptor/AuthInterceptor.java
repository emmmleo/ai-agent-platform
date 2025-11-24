package Maven.Project.auth.interceptor;

import Maven.Project.auth.util.JwtUtil;
import Maven.Project.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    private final JwtUtil jwtUtil;
    
    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // 获取 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        
        String token = authHeader.substring(7);
        
        // 验证 token
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "Token 无效或已过期");
        }
        
        // 将用户信息存入 request attribute
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("role", role);
        
        // 如果是管理员路径，验证是否为管理员
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/api/admin/")) {
            if (!"admin".equals(role)) {
                throw new BusinessException(403, "无权限访问");
            }
        }
        
        return true;
    }
}

