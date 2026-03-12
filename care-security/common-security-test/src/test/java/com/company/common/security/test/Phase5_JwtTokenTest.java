package com.company.common.security.test;

import com.company.common.security.captcha.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.security.RedisTokenBlacklistService;
import com.company.common.security.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.company.common.security.test.TestLoginHelper.loginRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@CareSecurityTest
@DisplayName("Phase 5: JWT Token - Generation and Validation")
class Phase5_JwtTokenTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private RedisTokenBlacklistService blacklistService;

    @Value("${care-test.admin-password}")
    private String adminPassword;

    private TokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        tokenResponse = authService.login(request, "127.0.0.1", "JUnit");
    }

    @Test
    @DisplayName("5.1 Access token is a valid JWT")
    void testAccessTokenIsValidJwt() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        assertThat(jwt).isNotNull();
        assertThat(jwt.getSubject()).isNotBlank();
    }

    @Test
    @DisplayName("5.2 Access token contains subject = username")
    void testAccessTokenContainsSubject() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("5.3 Access token contains roles claim")
    void testAccessTokenContainsRoles() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        List<String> roles = jwt.getClaimAsStringList("roles");
        assertThat(roles).isNotNull();
        assertThat(roles).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("5.4 Access token contains userId claim")
    void testAccessTokenContainsUserId() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        Object userId = jwt.getClaim("userId");
        assertThat(userId).isNotNull();
        assertThat(((Number) userId).longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("5.5 Access token contains cname claim")
    void testAccessTokenContainsCname() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        String cname = jwt.getClaimAsString("cname");
        assertThat(cname).isNotNull();
        assertThat(cname).isNotBlank();
    }

    @Test
    @DisplayName("5.6 Access token does NOT contain permissions (slimmed JWT)")
    void testAccessTokenDoesNotContainPermissions() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        Map<String, Object> permissions = jwt.getClaim("permissions");
        assertThat(permissions).isNull();
    }

    @Test
    @DisplayName("5.7 Access token type = 'access'")
    void testAccessTokenType() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        assertThat(jwt.getClaimAsString("token_type")).isEqualTo("access");
    }

    @Test
    @DisplayName("5.8 Refresh token type = 'refresh'")
    void testRefreshTokenType() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.refreshToken());
        assertThat(jwt.getClaimAsString("token_type")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("5.9 Refresh token has no permissions claim")
    void testRefreshTokenNoPermissions() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.refreshToken());
        assertThat((Object) jwt.getClaim("permissions")).isNull();
        assertThat((Object) jwt.getClaim("roles")).isNull();
    }

    @Test
    @DisplayName("5.10 Access token expires approximately at now + TTL")
    void testAccessTokenExpiry() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        Instant exp = jwt.getExpiresAt();
        assertThat(exp).isNotNull();
        Instant expected = Instant.now().plusSeconds(30 * 60);
        assertThat(exp).isBetween(
                expected.minusSeconds(5 * 60),
                expected.plusSeconds(5 * 60));
    }

    @Test
    @DisplayName("5.11 Refresh token expires approximately at now + TTL days")
    void testRefreshTokenExpiry() {
        Jwt jwt = jwtDecoder.decode(tokenResponse.refreshToken());
        Instant exp = jwt.getExpiresAt();
        assertThat(exp).isNotNull();
        Instant expected = Instant.now().plusSeconds(7 * 24 * 60 * 60);
        assertThat(exp).isBetween(
                expected.minusSeconds(3600),
                expected.plusSeconds(3600));
    }

    @Test
    @DisplayName("5.12 Refresh token rotation returns new tokens and blacklists old")
    @Transactional
    void testRefreshTokenRotation() {
        String oldRefreshToken = tokenResponse.refreshToken();
        Jwt oldJwt = jwtDecoder.decode(oldRefreshToken);
        String oldJti = oldJwt.getId();

        TokenResponse newResponse = authService.refresh(oldRefreshToken);

        assertThat(newResponse).isNotNull();
        assertThat(newResponse.accessToken()).isNotEqualTo(tokenResponse.accessToken());
        assertThat(newResponse.refreshToken()).isNotEqualTo(oldRefreshToken);
        assertThat(blacklistService.isBlacklisted(oldJti)).isTrue();
    }

    @Test
    @DisplayName("5.13 Refresh with blacklisted token throws BadCredentialsException")
    @Transactional
    void testRefreshWithBlacklistedToken() {
        String refreshToken = tokenResponse.refreshToken();
        authService.refresh(refreshToken);

        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("5.14 Refresh with access token fails")
    void testRefreshWithAccessTokenFails() {
        assertThatThrownBy(() -> authService.refresh(tokenResponse.accessToken()))
                .isInstanceOf(BadCredentialsException.class);
    }
}
