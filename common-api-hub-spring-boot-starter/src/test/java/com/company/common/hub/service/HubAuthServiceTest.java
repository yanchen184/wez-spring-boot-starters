package com.company.common.hub.service;

import com.company.common.hub.dto.HubAuthResult;
import com.company.common.hub.dto.HubResponseCode;
import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.entity.HubUserSet;
import com.company.common.hub.exception.HubAuthException;
import com.company.common.hub.exception.HubTokenException;
import com.company.common.hub.repository.HubSetRepository;
import com.company.common.hub.repository.HubUserRepository;
import com.company.common.hub.repository.HubUserSetRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HubAuthService 單元測試（4 層認證核心）。
 *
 * <p>Mock 所有 Repository 和 Service，按 4 層分組測試。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HubAuthService 測試 — 4 層認證核心")
class HubAuthServiceTest {

    @Mock
    private HubSetRepository hubSetRepository;

    @Mock
    private HubUserRepository hubUserRepository;

    @Mock
    private HubUserSetRepository hubUserSetRepository;

    @Mock
    private HubTokenService hubTokenService;

    @Mock
    private IpWhitelistService ipWhitelistService;

    private HubAuthService authService;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        authService = new HubAuthService(
                hubSetRepository,
                hubUserRepository,
                hubUserSetRepository,
                hubTokenService,
                ipWhitelistService,
                ENCODER
        );
    }

    // ===== Helper Methods =====

    private HubSet createHubSet(Long id, String uri, boolean enabled) {
        HubSet hubSet = new HubSet();
        hubSet.setId(id);
        hubSet.setName("Test API");
        hubSet.setUri(uri);
        hubSet.setEnabled(enabled);
        hubSet.setJwtTokenAging(3600);
        return hubSet;
    }

    private HubUser createHubUser(Long id, String username, String rawPassword, boolean enabled) {
        HubUser hubUser = new HubUser();
        hubUser.setId(id);
        hubUser.setUsername(username);
        hubUser.setPassword(ENCODER.encode(rawPassword));
        hubUser.setEnabled(enabled);
        return hubUser;
    }

    private HubUserSet createHubUserSet(HubUser user, HubSet set,
                                         boolean enabled,
                                         boolean userVerify,
                                         boolean jwtTokenVerify,
                                         LocalDate dts, LocalDate dte) {
        HubUserSet userSet = new HubUserSet();
        userSet.setId(1L);
        userSet.setHubUser(user);
        userSet.setHubSet(set);
        userSet.setEnabled(enabled);
        userSet.setUserVerify(userVerify);
        userSet.setJwtTokenVerify(jwtTokenVerify);
        userSet.setVerifyDts(dts);
        userSet.setVerifyDte(dte);
        return userSet;
    }

    // ===== Layer 1: URI 匹配 =====

    @Nested
    @DisplayName("Layer 1: URI 匹配")
    class UriMatching {

        @Test
        @DisplayName("URI 不在任何 HubSet 中時應拒絕")
        void shouldReject_whenUriNotRegistered() {
            when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/unknown", "GET", null, null, "some-token", "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.API_SET_DISABLED));
        }

        @Test
        @DisplayName("HubSet 停用時應拒絕（findByEnabledTrue 不會回傳停用的）")
        void shouldReject_whenHubSetDisabled() {
            // findByEnabledTrue 只回傳啟用的，停用的不會出現
            when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", null, null, "some-token", "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.API_SET_DISABLED));
        }

        @Test
        @DisplayName("應支援 Ant 風格 URI 匹配")
        void shouldMatch_antStyleUri() {
            HubSet hubSet = createHubSet(1L, "/api/users/**", true);
            HubUser hubUser = createHubUser(1L, "admin", "password123", true);
            HubUserSet hubUserSet = createHubUserSet(
                    hubUser, hubSet, true, false, true,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)
            );

            when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of(hubSet));

            Claims claims = new DefaultClaims(Map.of(
                    "userId", 1L, "hubSetId", 1L, "sub", "1"
            ));
            when(hubTokenService.validateToken("valid-token")).thenReturn(claims);
            when(hubTokenService.isBlacklisted("valid-token")).thenReturn(false);
            when(hubUserRepository.findById(1L)).thenReturn(Optional.of(hubUser));
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.of(hubUserSet));
            when(ipWhitelistService.isAllowed(anyString(), any())).thenReturn(true);

            HubAuthResult result = authService.authenticate(
                    "/api/users/123/profile", "GET", null, null, "valid-token", "127.0.0.1"
            );

            assertThat(result).isNotNull();
            assertThat(result.getHubSet().getUri()).isEqualTo("/api/users/**");
        }
    }

    // ===== Layer 2: 認證（帳密 / Token） =====

    @Nested
    @DisplayName("Layer 2: 認證")
    class Authentication {

        private HubSet hubSet;

        @BeforeEach
        void setUpHubSet() {
            hubSet = createHubSet(1L, "/api/users/**", true);
            when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of(hubSet));
        }

        @Test
        @DisplayName("帳密正確時應認證成功")
        void shouldAuthenticate_withValidPassword() {
            HubUser hubUser = createHubUser(1L, "admin", "password123", true);
            HubUserSet hubUserSet = createHubUserSet(
                    hubUser, hubSet, true, true, true,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)
            );

            when(hubUserRepository.findByUsername("admin")).thenReturn(Optional.of(hubUser));
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.of(hubUserSet));
            when(ipWhitelistService.isAllowed(anyString(), any())).thenReturn(true);

            HubAuthResult result = authService.authenticate(
                    "/api/users/1", "GET", "admin", "password123", null, "127.0.0.1"
            );

            assertThat(result.getHubUser().getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("密碼錯誤時應拒絕")
        void shouldReject_withWrongPassword() {
            HubUser hubUser = createHubUser(1L, "admin", "password123", true);
            when(hubUserRepository.findByUsername("admin")).thenReturn(Optional.of(hubUser));

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", "admin", "wrongPassword", null, "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.AUTH_FAILED));
        }

        @Test
        @DisplayName("帳號停用時應拒絕")
        void shouldReject_whenUserDisabled() {
            HubUser hubUser = createHubUser(1L, "admin", "password123", false);
            when(hubUserRepository.findByUsername("admin")).thenReturn(Optional.of(hubUser));

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", "admin", "password123", null, "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.AUTH_FAILED));
        }

        @Test
        @DisplayName("合法 Token 應認證成功")
        void shouldAuthenticate_withValidToken() {
            HubUser hubUser = createHubUser(1L, "admin", "password123", true);
            HubUserSet hubUserSet = createHubUserSet(
                    hubUser, hubSet, true, true, true,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)
            );

            Claims claims = new DefaultClaims(Map.of(
                    "userId", 1L, "hubSetId", 1L, "sub", "1"
            ));
            when(hubTokenService.validateToken("valid-token")).thenReturn(claims);
            when(hubTokenService.isBlacklisted("valid-token")).thenReturn(false);
            when(hubUserRepository.findById(1L)).thenReturn(Optional.of(hubUser));
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.of(hubUserSet));
            when(ipWhitelistService.isAllowed(anyString(), any())).thenReturn(true);

            HubAuthResult result = authService.authenticate(
                    "/api/users/1", "GET", null, null, "valid-token", "127.0.0.1"
            );

            assertThat(result.getHubUser().getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("過期 Token 應拒絕")
        void shouldReject_withExpiredToken() {
            when(hubTokenService.validateToken("expired-token"))
                    .thenThrow(new HubTokenException("422002", "Token 已過期"));

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", null, null, "expired-token", "127.0.0.1"
            ))
                    .isInstanceOf(HubTokenException.class)
                    .satisfies(ex -> assertThat(((HubTokenException) ex).getCode())
                            .isEqualTo(HubResponseCode.TOKEN_EXPIRED));
        }

        @Test
        @DisplayName("沒帶帳密也沒帶 Token 時應拒絕")
        void shouldReject_whenNoCredentials() {
            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", null, null, null, "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.AUTH_FAILED));
        }

        @Test
        @DisplayName("帳號不存在時應拒絕")
        void shouldReject_whenUserNotFound() {
            when(hubUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", "ghost", "password", null, "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.AUTH_FAILED));
        }

        @Test
        @DisplayName("Token 在黑名單中時應拒絕")
        void shouldReject_whenTokenBlacklisted() {
            Claims claims = new DefaultClaims(Map.of(
                    "userId", 1L, "hubSetId", 1L, "sub", "1"
            ));
            when(hubTokenService.validateToken("blacklisted-token")).thenReturn(claims);
            when(hubTokenService.isBlacklisted("blacklisted-token")).thenReturn(true);

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", null, null, "blacklisted-token", "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.TOKEN_INVALID));
        }
    }

    // ===== Layer 3: 授權 =====

    @Nested
    @DisplayName("Layer 3: 授權")
    class Authorization {

        private HubSet hubSet;
        private HubUser hubUser;

        @BeforeEach
        void setUpCommon() {
            hubSet = createHubSet(1L, "/api/users/**", true);
            hubUser = createHubUser(1L, "admin", "password123", true);
            when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of(hubSet));
        }

        /** 帳密測試共用 stub。 */
        private void stubPasswordAuth() {
            when(hubUserRepository.findByUsername("admin")).thenReturn(Optional.of(hubUser));
        }

        @Test
        @DisplayName("無 HubUserSet 映射時應拒絕")
        void shouldReject_whenNoUserSetMapping() {
            stubPasswordAuth();
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", "admin", "password123", null, "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.AUTH_FAILED));
        }

        @Test
        @DisplayName("HubUserSet 停用時應拒絕（findByEnabledTrue 不回傳）")
        void shouldReject_whenUserSetDisabled() {
            stubPasswordAuth();
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", "admin", "password123", null, "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.AUTH_FAILED));
        }

        @Test
        @DisplayName("不在有效日期範圍內時應拒絕")
        void shouldReject_whenOutsideValidDateRange() {
            stubPasswordAuth();
            HubUserSet hubUserSet = createHubUserSet(
                    hubUser, hubSet, true, true, true,
                    LocalDate.now().minusDays(30), LocalDate.now().minusDays(1)
            );
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.of(hubUserSet));

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", "admin", "password123", null, "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.AUTH_FAILED));
        }

        @Test
        @DisplayName("userVerify=false 但使用帳密時應拒絕")
        void shouldReject_whenUserVerifyFalse_butUsedPassword() {
            stubPasswordAuth();
            HubUserSet hubUserSet = createHubUserSet(
                    hubUser, hubSet, true, false, true,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)
            );
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.of(hubUserSet));

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", "admin", "password123", null, "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.AUTH_FAILED));
        }

        @Test
        @DisplayName("jwtTokenVerify=false 但使用 Token 時應拒絕")
        void shouldReject_whenJwtVerifyFalse_butUsedToken() {
            HubUserSet hubUserSet = createHubUserSet(
                    hubUser, hubSet, true, true, false,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)
            );

            Claims claims = new DefaultClaims(Map.of(
                    "userId", 1L, "hubSetId", 1L, "sub", "1"
            ));
            when(hubTokenService.validateToken("valid-token")).thenReturn(claims);
            when(hubTokenService.isBlacklisted("valid-token")).thenReturn(false);
            when(hubUserRepository.findById(1L)).thenReturn(Optional.of(hubUser));
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.of(hubUserSet));

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", null, null, "valid-token", "127.0.0.1"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.AUTH_FAILED));
        }

        @Test
        @DisplayName("兩種驗證都啟用且使用帳密時應放行")
        void shouldAllow_whenBothVerifyTrue() {
            stubPasswordAuth();
            HubUserSet hubUserSet = createHubUserSet(
                    hubUser, hubSet, true, true, true,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)
            );
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.of(hubUserSet));
            when(ipWhitelistService.isAllowed(anyString(), any())).thenReturn(true);

            HubAuthResult result = authService.authenticate(
                    "/api/users/1", "GET", "admin", "password123", null, "127.0.0.1"
            );

            assertThat(result).isNotNull();
            assertThat(result.getHubUser().getUsername()).isEqualTo("admin");
            assertThat(result.getHubSet().getUri()).isEqualTo("/api/users/**");
            assertThat(result.getHubUserSet()).isNotNull();
        }
    }

    // ===== Layer 4: IP 白名單 =====

    @Nested
    @DisplayName("Layer 4: IP 白名單")
    class IpWhitelist {

        private HubSet hubSet;
        private HubUser hubUser;

        @BeforeEach
        void setUpCommon() {
            hubSet = createHubSet(1L, "/api/users/**", true);
            hubUser = createHubUser(1L, "admin", "password123", true);
            hubUser.setVerifyIp("10.0.0.0/8");

            when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of(hubSet));
            when(hubUserRepository.findByUsername("admin")).thenReturn(Optional.of(hubUser));
        }

        @Test
        @DisplayName("IP 不在白名單時應拒絕")
        void shouldReject_whenIpNotInWhitelist() {
            HubUserSet hubUserSet = createHubUserSet(
                    hubUser, hubSet, true, true, true,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)
            );
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.of(hubUserSet));
            when(ipWhitelistService.isAllowed("192.168.1.100", "10.0.0.0/8")).thenReturn(false);

            assertThatThrownBy(() -> authService.authenticate(
                    "/api/users/1", "GET", "admin", "password123", null, "192.168.1.100"
            ))
                    .isInstanceOf(HubAuthException.class)
                    .satisfies(ex -> assertThat(((HubAuthException) ex).getCode())
                            .isEqualTo(HubResponseCode.IP_DENIED));
        }

        @Test
        @DisplayName("白名單為空時應允許（IpWhitelistService 會回傳 true）")
        void shouldAllow_whenWhitelistEmpty() {
            hubUser.setVerifyIp(null);

            HubUserSet hubUserSet = createHubUserSet(
                    hubUser, hubSet, true, true, true,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)
            );
            when(hubUserSetRepository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet))
                    .thenReturn(Optional.of(hubUserSet));
            when(ipWhitelistService.isAllowed("192.168.1.100", null)).thenReturn(true);

            HubAuthResult result = authService.authenticate(
                    "/api/users/1", "GET", "admin", "password123", null, "192.168.1.100"
            );

            assertThat(result).isNotNull();
        }
    }
}
