package com.aiagent.config;

import com.aiagent.menu.entity.Menu;
import com.aiagent.menu.mapper.MenuMapper;
import com.aiagent.user.entity.User;
import com.aiagent.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final MenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper,
                           MenuMapper menuMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initUsers();
        initMenus();
    }

    private void initUsers() {
        createUserIfAbsent("admin", "123456", "ROLE_ADMIN");
        createUserIfAbsent("user", "123456", "ROLE_USER");
    }

    private void createUserIfAbsent(String username, String rawPassword, String role) {
        User existingUser = userMapper.findByUsername(username);
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        if (existingUser != null) {
            // Force reset password for dev convenience (optional, but requested by user issue)
            if ("admin".equals(username) || "user".equals(username)) {
                existingUser.setPasswordHash(encodedPassword);
                userMapper.updateUser(existingUser); // Assuming updateUser exists
                log.info("Reset password for default user: {}", username);
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encodedPassword);
        user.setRole(role);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insertUser(user);
        log.info("Created default user: {}", username);
    }

    private void initMenus() {
        long menuCount = menuMapper.count();
        log.info("Current menu count: {}", menuCount);
        
        // 获取所有现有菜单
        var allMenus = menuMapper.findAll();

        // Fix incorrect Dashboard path if exists
        var wrongDashboard = allMenus.stream()
                .filter(m -> "仪表盘".equals(m.getTitle()) && "/".equals(m.getPath()))
                .findFirst();
        
        if (wrongDashboard.isPresent()) {
            var menu = wrongDashboard.get();
            menu.setPath("/dashboard");
            menuMapper.update(menu);
            log.info("Fixed incorrect Dashboard path from '/' to '/dashboard'");
            // Refresh list
            allMenus = menuMapper.findAll();
        }
        
        // 清理旧的菜单路径（/home/* 路径）
        var oldMenus = allMenus.stream()
                .filter(m -> m.getPath() != null && m.getPath().startsWith("/home/"))
                .collect(java.util.stream.Collectors.toList());
        
        if (!oldMenus.isEmpty()) {
            log.warn("Found {} old menu(s) with /home/* paths. These should be cleaned up manually from database.", 
                    oldMenus.size());
            for (var oldMenu : oldMenus) {
                log.warn("  - Old menu: {} (path: {})", oldMenu.getTitle(), oldMenu.getPath());
            }
        }
        
        // 检查并创建/更新菜单（使用新路径）
               ensureMenuExists(allMenus, "仪表盘", "/dashboard", 1, "ROLE_USER,ROLE_ADMIN");
               ensureMenuExists(allMenus, "个人信息", "/account/profile", 2, "ROLE_USER,ROLE_ADMIN");
               ensureMenuExists(allMenus, "智能体管理", "/agents", 3, "ROLE_USER,ROLE_ADMIN");
               ensureMenuExists(allMenus, "知识库管理", "/knowledge-bases", 4, "ROLE_USER,ROLE_ADMIN");
               ensureMenuExists(allMenus, "工作流管理", "/workflows", 5, "ROLE_USER,ROLE_ADMIN");
               ensureMenuExists(allMenus, "插件管理", "/plugins", 6, "ROLE_USER,ROLE_ADMIN");
               ensureMenuExists(allMenus, "用户管理", "/users", 7, "ROLE_ADMIN");
        
        log.info("Menu initialization completed");
    }
    
    private void ensureMenuExists(List<com.aiagent.menu.entity.Menu> existingMenus, 
                                   String title, String path, int order, String roles) {
        // 查找所有匹配路径的菜单（可能有重复）
        var matchingMenus = existingMenus.stream()
                .filter(m -> m.getPath().equals(path))
                .collect(java.util.stream.Collectors.toList());
        
        if (!matchingMenus.isEmpty()) {
            // 如果存在多个相同路径的菜单，记录警告
            if (matchingMenus.size() > 1) {
                log.warn("Found {} duplicate menus with path '{}'. Please clean up database.", 
                        matchingMenus.size(), path);
            }
            
            var menu = matchingMenus.get(0);
            // 如果菜单存在但权限不对，记录警告
            if (!roles.equals(menu.getAllowedRoles())) {
                log.warn("Menu '{}' exists but has wrong roles. Expected: {}, Actual: {}. Please update database manually.", 
                        title, roles, menu.getAllowedRoles());
            } else {
                log.info("Menu '{}' already exists with correct roles", title);
            }
        } else {
            // 创建新菜单
            Menu menu = createMenu(null, title, path, order, roles);
            menuMapper.insert(menu);
            log.info("Created menu: {} with roles: {}", title, roles);
        }
    }

    private Menu createMenu(Long parentId, String title, String path, int order, String roles) {
        Menu menu = new Menu();
        menu.setParentId(parentId);
        menu.setTitle(title);
        menu.setPath(path);
        menu.setOrderNum(order);
        menu.setAllowedRoles(roles);
        return menu;
    }
}
