package com.aiagent.user.controller;

import com.aiagent.user.dto.UserProfileResponse;
import com.aiagent.user.entity.User;
import com.aiagent.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(principal.getUsername());
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

