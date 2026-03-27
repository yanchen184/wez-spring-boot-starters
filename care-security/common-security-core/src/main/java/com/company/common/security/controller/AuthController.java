package com.company.common.security.controller;

import com.company.common.security.dto.request.ChangePasswordRequest;
import com.company.common.security.dto.request.LoginRequest;
import com.company.common.security.dto.request.LogoutRequest;
import com.company.common.security.dto.request.RefreshTokenRequest;
import com.company.common.security.dto.request.SwitchUserRequest;
import com.company.common.response.dto.ApiResponse;
import com.company.common.security.dto.response.MyOrgResponse;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.dto.response.UserInfoResponse;
import com.company.common.security.security.CustomUserDetails;
import com.company.common.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Auth", description = "Authentication and token management")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = "Authenticate with username/password and receive JWT tokens")
    @PostMapping("/login")
    public TokenResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = com.company.common.security.util.IpUtils.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return authService.login(request, ipAddress, userAgent);
    }

    @Operation(summary = "Refresh token", description = "Exchange a valid refresh token for a new access token")
    @PostMapping("/refresh")
    public TokenResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "Logout", description = "Invalidate refresh token")
    @PostMapping("/logout")
    public void logout(@RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
    }

    @Operation(summary = "Change password", description = "Change the authenticated user's password")
    @PostMapping("/change-password")
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
    }

    @Operation(summary = "Get current user info", description = "Returns user profile, roles, org roles, and CRUD permissions")
    @GetMapping("/me")
    public UserInfoResponse me(Authentication authentication) {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        Long currentOrgId = jwt.hasClaim("currentOrgId") ? jwt.getClaim("currentOrgId") : null;
        CustomUserDetails details = authService.getUserInfo(authentication.getName(), currentOrgId);
        return UserInfoResponse.from(details);
    }

    @Operation(summary = "Get my organizations", description = "List all organizations the current user belongs to")
    @GetMapping("/my-orgs")
    public List<MyOrgResponse> getMyOrgs(Authentication authentication) {
        return authService.getMyOrgs(authentication.getName());
    }

    @Operation(summary = "Switch user (impersonate)", description = "Admin impersonates another user. Requires ROLE_ADMIN.")
    @PostMapping("/switch-user")
    public TokenResponse switchUser(
            @Valid @RequestBody SwitchUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String ipAddress = com.company.common.security.util.IpUtils.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return authService.switchUser(
                authentication.getName(), request.username(), request.orgId(), ipAddress, userAgent);
    }

    @Operation(summary = "Exit switch user", description = "Return to the original admin account after impersonation")
    @PostMapping("/exit-switch-user")
    public ResponseEntity<ApiResponse<TokenResponse>> exitSwitchUser(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        String impersonatedBy = jwt.getClaimAsString("impersonatedBy");
        if (impersonatedBy == null || impersonatedBy.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Not currently impersonating any user"));
        }
        String ipAddress = com.company.common.security.util.IpUtils.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String currentUser = authentication.getName();
        TokenResponse token = authService.exitSwitchUser(impersonatedBy, currentUser, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

}
