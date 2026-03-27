package com.company.common.hub.controller;

import com.company.common.hub.IntegrationTestConfig;
import com.company.common.hub.TestJpaConfig;
import com.company.common.hub.dto.HubAuthResult;
import com.company.common.hub.dto.HubResponseCode;
import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.entity.HubUserSet;
import com.company.common.hub.exception.HubAuthException;
import com.company.common.hub.exception.HubTokenException;
import com.company.common.hub.service.HubAuthService;
import com.company.common.hub.service.HubLogService;
import com.company.common.hub.service.HubTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HubTokenController 測試。
 *
 * <p>使用 @SpringBootTest + @AutoConfigureMockMvc + MockitoBean。
 */
@SpringBootTest(classes = {TestJpaConfig.class, IntegrationTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HubTokenController 測試")
class HubTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HubAuthService hubAuthService;

    @MockitoBean
    private HubTokenService hubTokenService;

    @MockitoBean
    private HubLogService hubLogService;

    private HubAuthResult createAuthResult() {
        HubSet hubSet = new HubSet();
        hubSet.setId(1L);
        hubSet.setUri("/api/users/**");
        hubSet.setJwtTokenAging(3600);

        HubUser hubUser = new HubUser();
        hubUser.setId(1L);
        hubUser.setUsername("admin");
        hubUser.setOrgId(100L);

        HubUserSet hubUserSet = new HubUserSet();
        hubUserSet.setId(1L);

        return new HubAuthResult(hubUser, hubSet, hubUserSet);
    }

    @Test
    @DisplayName("帳密正確時應回傳 Token")
    void shouldIssueToken_whenCredentialsValid() throws Exception {
        HubAuthResult authResult = createAuthResult();
        when(hubAuthService.authenticate(eq("/api/users/**"), eq("POST"),
                eq("admin"), eq("password123"), isNull(), anyString()))
                .thenReturn(authResult);
        when(hubTokenService.issueToken(1L, 100L, 1L, 3600))
                .thenReturn("jwt-token-string");

        String body = "{\"username\":\"admin\",\"password\":\"password123\",\"uri\":\"/api/users/**\"}";

        mockMvc.perform(post("/api/hub/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HubResponseCode.TOKEN_ISSUED))
                .andExpect(jsonPath("$.data.token").value("jwt-token-string"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    @DisplayName("帳密錯誤時應回傳 401")
    void shouldReturn401_whenCredentialsInvalid() throws Exception {
        when(hubAuthService.authenticate(any(), any(), any(), any(), any(), any()))
                .thenThrow(new HubAuthException(HubResponseCode.AUTH_FAILED, "密碼錯誤"));

        String body = "{\"username\":\"admin\",\"password\":\"wrong\",\"uri\":\"/api/users/**\"}";

        mockMvc.perform(post("/api/hub/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(HubResponseCode.AUTH_FAILED));
    }

    @Test
    @DisplayName("Token 續期成功時應回傳新 Token")
    void shouldRefreshToken_whenTokenValid() throws Exception {
        Claims claims = new DefaultClaims(Map.of(
                "userId", 1L, "orgId", 100L, "hubSetId", 1L, "sub", "1"
        ));
        when(hubTokenService.validateToken("old-token")).thenReturn(claims);
        when(hubTokenService.isBlacklisted("old-token")).thenReturn(false);
        when(hubTokenService.issueToken(1L, 100L, 1L, 3600))
                .thenReturn("new-jwt-token");

        mockMvc.perform(post("/api/hub/token/refresh")
                        .header("X-Hub-Token", "old-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HubResponseCode.TOKEN_REFRESHED))
                .andExpect(jsonPath("$.data.token").value("new-jwt-token"));
    }

    @Test
    @DisplayName("過期 Token 續期時應回傳 422")
    void shouldReturn422_whenRefreshTokenExpired() throws Exception {
        when(hubTokenService.validateToken("expired-token"))
                .thenThrow(new HubTokenException(HubResponseCode.TOKEN_EXPIRED, "Token 已過期"));

        mockMvc.perform(post("/api/hub/token/refresh")
                        .header("X-Hub-Token", "expired-token"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(HubResponseCode.TOKEN_EXPIRED));
    }
}
