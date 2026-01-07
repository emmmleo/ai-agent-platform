package com.aiagent.security;

import com.aiagent.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT工具类
 */
@Component
public class JwtUtils {

    @Autowired
    private JwtConfig jwtConfig;

    /**
     * 生成JWT令牌
     */
    public String generateToken(Authentication authentication, Map<String, Object> extraClaims) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Map<String, Object> claims = new java.util.HashMap<>();
        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }
        claims.put("username", userDetails.getUsername());
        claims.put("roles", extractAuthorities(userDetails.getAuthorities()));

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = buildParser()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * 从令牌中解析角色列表
     */
    public List<String> getRolesFromToken(String token) {
        Claims claims = buildParser()
                .parseSignedClaims(token)
                .getPayload();
        Object roles = claims.get("roles");
        if (roles instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            buildParser().parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            // 令牌格式错误、不支持的令牌、参数错误
            return false;
        } catch (ExpiredJwtException e) {
            // 令牌过期
            return false;
        }
    }

    /**
     * 从请求头中提取令牌
     */
    public String extractTokenFromHeader(String header) {
        if (header != null && header.startsWith(jwtConfig.getPrefix())) {
            return header.substring(jwtConfig.getPrefix().length()).trim();
        }
        return null;
    }

    /**
     * 从请求中获取用户ID
     */
    public Long getUserIdFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String header = request.getHeader(jwtConfig.getHeader());
        String token = extractTokenFromHeader(header);
        if (token != null && validateToken(token)) {
            Claims claims = buildParser().parseSignedClaims(token).getPayload();
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            } else if (userId instanceof Long) {
                return (Long) userId;
            }
        }
        return null;
    }

    private List<String> extractAuthorities(Collection<?> authorities) {
        return authorities.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    private io.jsonwebtoken.JwtParser buildParser() {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
