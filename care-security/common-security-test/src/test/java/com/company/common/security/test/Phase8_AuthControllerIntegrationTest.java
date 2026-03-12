package com.company.common.security.test;

import tools.jackson.databind.ObjectMapper;
import com.company.common.security.captcha.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;
import com.company.common.security.dto.request.LogoutRequest;
import com.company.common.security.dto.request.RefreshTokenRequest;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.company.common.security.test.TestLoginHelper.loginRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@CareSecurityTest
@AutoConfigureMockMvc
@Import({com.company.common.security.controller.AuthController.class,
         com.company.common.security.controller.UserController.class,
         com.company.common.security.controller.RoleController.class,
         com.company.common.security.exception.SecurityExceptionHandler.class})
@DisplayName("Phase 8: Auth Controller Integration - MockMvc")
class Phase8_AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @Value("${care-test.admin-password}")
    private String adminPassword;

    @Value("${care-test.non-admin-username}")
    private String nonAdminUsername;

    private String adminAccessToken;
    private String adminRefreshToken;

    @BeforeEach
    void setUp() {
        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        TokenResponse response = authService.login(request, "127.0.0.1", "JUnit");
        adminAccessToken = response.accessToken();
        adminRefreshToken = response.refreshToken();
    }

    @Test
    @DisplayName("8.1 POST /api/auth/login with correct credentials returns 200 + accessToken")
    @Transactional
    void testLoginEndpoint() throws Exception {
        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("8.2 POST /api/auth/login with wrong password returns 401")
    @Transactional
    void testLoginEndpointWrongPassword() throws Exception {
        LoginRequest request = loginRequest("ADMIN", "WrongPassword!", captchaService);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8.3 POST /api/auth/login with missing username returns 400")
    void testLoginEndpointMissingUsername() throws Exception {
        String body = "{\"password\": \"test\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8.4 POST /api/auth/refresh returns 200 + new token")
    @Transactional
    void testRefreshEndpoint() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(adminRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("8.5 POST /api/auth/logout returns 200")
    @Transactional
    void testLogoutEndpoint() throws Exception {
        LogoutRequest request = new LogoutRequest(adminRefreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("8.6 GET /api/users without token returns 401")
    void testProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8.7 Authenticated request to /api/** with valid JWT succeeds (not 401)")
    void testProtectedWithValidToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LogoutRequest(adminRefreshToken))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("8.8 Request with invalid token returns 401")
    void testProtectedWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8.9 Role-based access: ADMIN passes URL-level hasRole for /api/roles")
    void testRoleBasedAccessAdmin() throws Exception {
        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("8.10 Role-based access denied: non-admin cannot access /api/roles")
    @Transactional
    void testRoleBasedAccessDenied() throws Exception {
        LoginRequest request = loginRequest(nonAdminUsername, adminPassword, captchaService);
        TokenResponse response = authService.login(request, "127.0.0.1", "JUnit");

        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + response.accessToken()))
                .andExpect(status().isForbidden());
    }
}
