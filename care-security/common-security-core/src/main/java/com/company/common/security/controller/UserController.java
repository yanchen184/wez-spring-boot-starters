package com.company.common.security.controller;

import com.company.common.security.dto.request.AssignOrgRoleRequest;
import com.company.common.security.dto.request.CreateUserRequest;
import com.company.common.security.dto.request.ResetPasswordRequest;
import com.company.common.security.dto.request.UpdateUserRequest;
import com.company.common.response.dto.ApiResponse;
import com.company.common.security.dto.response.UserResponse;
import com.company.common.security.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User", description = "User management (requires ADMIN or USER_ADMIN role)")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List all users", description = "Retrieve all user accounts, optionally filtered by organization")
    @GetMapping
public ResponseEntity<ApiResponse<List<UserResponse>>> findAll(
            @RequestParam(required = false) Long orgId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findAllByOrg(orgId)));
    }

    @Operation(summary = "Get user by ID", description = "Retrieve a single user by their ID")
    @GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    @Operation(summary = "Create user", description = "Create a new user account")
    @PostMapping
public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        UserResponse user = userService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User created", user));
    }

    @Operation(summary = "Update user", description = "Update an existing user's information")
    @PutMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        UserResponse user = userService.update(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("User updated", user));
    }

    @Operation(summary = "Lock user", description = "Lock a user account to prevent login")
    @PostMapping("/{id}/lock")
public ResponseEntity<ApiResponse<Void>> lock(
            @PathVariable Long id, Authentication authentication) {
        userService.lockUser(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("User locked", null));
    }

    @Operation(summary = "Unlock user", description = "Unlock a previously locked user account")
    @PostMapping("/{id}/unlock")
public ResponseEntity<ApiResponse<Void>> unlock(
            @PathVariable Long id, Authentication authentication) {
        userService.unlockUser(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("User unlocked", null));
    }

    @Operation(summary = "Reset password", description = "Reset a user's password (admin operation)")
    @PostMapping("/{id}/reset-password")
public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request,
            Authentication authentication) {
        userService.resetPassword(id, request.newPassword(), authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Password reset", null));
    }

    @Operation(summary = "Get user org roles", description = "Retrieve organization-specific roles for a user")
    @GetMapping("/{id}/org-roles")
public ResponseEntity<ApiResponse<List<UserResponse.OrgRoleInfo>>> getOrgRoles(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getOrgRoles(id)));
    }

    @Operation(summary = "Assign org role", description = "Assign an organization-specific role to a user")
    @PostMapping("/{id}/org-roles")
public ResponseEntity<ApiResponse<UserResponse.OrgRoleInfo>> assignOrgRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignOrgRoleRequest request,
            Authentication authentication) {
        UserResponse.OrgRoleInfo result = userService.assignOrgRole(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Org role assigned", result));
    }

    @Operation(summary = "Remove org role", description = "Remove an organization-specific role from a user")
    @DeleteMapping("/{id}/org-roles/{orgRoleId}")
public ResponseEntity<ApiResponse<Void>> removeOrgRole(
            @PathVariable Long id,
            @PathVariable Long orgRoleId,
            Authentication authentication) {
        userService.removeOrgRole(id, orgRoleId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Org role removed", null));
    }
}
