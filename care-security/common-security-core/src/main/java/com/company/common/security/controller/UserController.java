package com.company.common.security.controller;

import com.company.common.response.dto.PageRequest;
import com.company.common.response.dto.PageResponse;
import com.company.common.security.dto.request.AssignOrgRoleRequest;
import com.company.common.security.dto.request.CreateUserRequest;
import com.company.common.security.dto.request.ResetPasswordRequest;
import com.company.common.security.dto.request.UpdateUserRequest;
import com.company.common.security.dto.response.UserResponse;
import com.company.common.security.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @Operation(summary = "Search users (paged)",
            description = "分頁搜尋使用者，支援 keyword 模糊比對（username / cname / email）、機構篩選、排序")
    @GetMapping
    public PageResponse<UserResponse> search(
            @Parameter(description = "模糊搜尋關鍵字（比對 username、cname、email）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "機構 ID（篩選指定機構的使用者）")
            @RequestParam(required = false) Long orgId,
            PageRequest pageRequest) {
        return userService.search(keyword, orgId, pageRequest);
    }

    @Operation(summary = "Get user by ID", description = "Retrieve a single user by their ID")
    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @Operation(summary = "Create user", description = "Create a new user account")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        return userService.create(request, authentication.getName());
    }

    @Operation(summary = "Update user", description = "Update an existing user's information")
    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        return userService.update(id, request, authentication.getName());
    }

    @Operation(summary = "Lock user", description = "Lock a user account to prevent login")
    @PostMapping("/{id}/lock")
    public void lock(
            @PathVariable Long id, Authentication authentication) {
        userService.lockUser(id, authentication.getName());
    }

    @Operation(summary = "Unlock user", description = "Unlock a previously locked user account")
    @PostMapping("/{id}/unlock")
    public void unlock(
            @PathVariable Long id, Authentication authentication) {
        userService.unlockUser(id, authentication.getName());
    }

    @Operation(summary = "Reset password", description = "Reset a user's password (admin operation)")
    @PostMapping("/{id}/reset-password")
    public void resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request,
            Authentication authentication) {
        userService.resetPassword(id, request.newPassword(), authentication.getName());
    }

    @Operation(summary = "Get user org roles", description = "Retrieve organization-specific roles for a user")
    @GetMapping("/{id}/org-roles")
    public List<UserResponse.OrgRoleInfo> getOrgRoles(
            @PathVariable Long id) {
        return userService.getOrgRoles(id);
    }

    @Operation(summary = "Assign org role", description = "Assign an organization-specific role to a user")
    @PostMapping("/{id}/org-roles")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse.OrgRoleInfo assignOrgRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignOrgRoleRequest request,
            Authentication authentication) {
        return userService.assignOrgRole(id, request, authentication.getName());
    }

    @Operation(summary = "Remove org role", description = "Remove an organization-specific role from a user")
    @DeleteMapping("/{id}/org-roles/{orgRoleId}")
    public void removeOrgRole(
            @PathVariable Long id,
            @PathVariable Long orgRoleId,
            Authentication authentication) {
        userService.removeOrgRole(id, orgRoleId, authentication.getName());
    }
}
