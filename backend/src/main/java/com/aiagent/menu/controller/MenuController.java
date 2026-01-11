package com.aiagent.menu.controller;

import com.aiagent.menu.dto.MenuResponse;
import com.aiagent.menu.service.MenuService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menus")
    public List<MenuResponse> menus(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // 详细调试日志
        System.out.println("=== Menu Request ===");
        System.out.println("Username: " + userDetails.getUsername());
        System.out.println("Current user roles: " + roles);
        
        List<MenuResponse> menus = menuService.getMenusForRoles(roles);
        
        System.out.println("Returned menus count: " + menus.size());
        for (MenuResponse menu : menus) {
            System.out.println("  - Menu: " + menu.getTitle() + " (path: " + menu.getPath() + ")");
        }
        System.out.println("===================");
        
        return menus;
    }
}

