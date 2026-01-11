package com.aiagent.user.controller;

import com.aiagent.user.dto.UserProfileResponse;
import com.aiagent.user.dto.UserProfileUpdateRequest;
import com.aiagent.user.entity.User;
import com.aiagent.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(principal.getUsername());
        UserProfileResponse resp = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        resp.setSchool(user.getSchool());
        resp.setPhone(user.getPhone());
        resp.setEmail(user.getEmail());
        resp.setBio(user.getBio());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setGender(user.getGender());
        resp.setBirthday(user.getBirthday());
        return resp;
    }

    @PatchMapping("/profile")
    public UserProfileResponse updateProfile(Authentication authentication,
                                             @Validated @RequestBody UserProfileUpdateRequest request) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User updated = userService.updateProfile(principal.getUsername(), request);
        UserProfileResponse resp = new UserProfileResponse(
                updated.getId(),
                updated.getUsername(),
                updated.getRole(),
                updated.getCreatedAt(),
                updated.getUpdatedAt()
        );
        resp.setSchool(updated.getSchool());
        resp.setPhone(updated.getPhone());
        resp.setEmail(updated.getEmail());
        resp.setBio(updated.getBio());
        resp.setAvatarUrl(updated.getAvatarUrl());
        resp.setGender(updated.getGender());
        resp.setBirthday(updated.getBirthday());
        return resp;
    }
}
