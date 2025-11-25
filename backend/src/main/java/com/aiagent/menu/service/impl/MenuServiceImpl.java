package com.aiagent.menu.service.impl;

import com.aiagent.menu.dto.MenuResponse;
import com.aiagent.menu.entity.Menu;
import com.aiagent.menu.mapper.MenuMapper;
import com.aiagent.menu.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;

    public MenuServiceImpl(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Override
    public List<MenuResponse> getMenusForRoles(List<String> roles) {
        List<Menu> allMenus = menuMapper.findAll();
        Set<String> roleSet = roles == null ? Set.of() : roles.stream().collect(Collectors.toSet());

        // 去重：基于 path 去重，保留每个 path 的第一条记录
        // 同时过滤掉旧的 /home/* 路径的菜单
        Map<String, Menu> uniqueMenus = new HashMap<>();
        for (Menu menu : allMenus) {
            String path = menu.getPath();
            
            // 跳过旧的 /home/* 路径的菜单
            if (path != null && path.startsWith("/home/")) {
                System.out.println("Skipping old menu: " + menu.getTitle() + " (path: " + path + ")");
                continue;
            }
            
            if (!uniqueMenus.containsKey(path)) {
                uniqueMenus.put(path, menu);
            } else {
                // 如果已存在，保留 ID 较小的（通常是先创建的）
                Menu existing = uniqueMenus.get(path);
                if (menu.getId() < existing.getId()) {
                    uniqueMenus.put(path, menu);
                }
            }
        }
        
        List<Menu> deduplicatedMenus = new ArrayList<>(uniqueMenus.values());
        
        // 调试日志
        System.out.println("All menus count (before dedup): " + allMenus.size());
        System.out.println("Unique menus count (after dedup): " + deduplicatedMenus.size());
        System.out.println("User roles: " + roleSet);

        List<Menu> filtered = deduplicatedMenus.stream()
                .filter(menu -> {
                    List<String> allowed = menu.getAllowedRoleList();
                    System.out.println("Menu: " + menu.getTitle() + ", Allowed roles: " + allowed);
                    if (allowed.isEmpty()) {
                        return true;
                    }
                    if (roleSet.isEmpty()) {
                        return false;
                    }
                    for (String role : allowed) {
                        if (roleSet.contains(role)) {
                            System.out.println("Menu " + menu.getTitle() + " matched role: " + role);
                            return true;
                        }
                    }
                    return false;
                })
                .sorted(Comparator.comparing(Menu::getParentId, Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(menu -> menu.getOrderNum() == null ? 0 : menu.getOrderNum())
                        .thenComparing(Menu::getId))
                .collect(Collectors.toList());

        Map<Long, MenuResponse> responseMap = new HashMap<>();
        List<MenuResponse> roots = new ArrayList<>();

        for (Menu menu : filtered) {
            MenuResponse node = new MenuResponse(menu.getId(), menu.getTitle(), menu.getPath());
            responseMap.put(menu.getId(), node);
        }

        for (Menu menu : filtered) {
            MenuResponse node = responseMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null) {
                roots.add(node);
            } else {
                MenuResponse parent = responseMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }

        return roots;
    }
}

