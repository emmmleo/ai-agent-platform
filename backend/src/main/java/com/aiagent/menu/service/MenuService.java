package com.aiagent.menu.service;

import com.aiagent.menu.dto.MenuResponse;

import java.util.List;

public interface MenuService {

    List<MenuResponse> getMenusForRoles(List<String> roles);
}

