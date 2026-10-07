package com.foodexpress.controller;

import com.foodexpress.dto.ApiResponse;
import com.foodexpress.dto.ChangePasswordRequest;
import com.foodexpress.dto.UserProfileDto;
import com.foodexpress.security.SecurityUtils;
import com.foodexpress.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Customer Profile", description = "User Profile and Account Management APIs")
public class CustomerController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    public CustomerController(UserService userService, SecurityUtils securityUtils) {
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", userService.getProfile(userId)));
    }

    @PutMapping
    @Operation(summary = "Update profile details")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(@Valid @RequestBody UserProfileDto dto) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateProfile(userId, dto)));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
