package com.company.common.security.test;

import com.company.common.security.captcha.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.security.RedisTokenBlacklistService;
import com.company.common.security.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.company.common.security.test.TestLoginHelper.loginRequest;
import static org.assertj.core.api.Assertions.assertThat;

@CareSecurityTest
@DisplayName("Phase 7: Redis Token Blacklist")
class Phase7_RedisBlacklistTest {

    @Autowired
    private RedisTokenBlacklistService blacklistService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Value("${care-test.admin-password}")
    private String adminPassword;

    @Test
    @DisplayName("7.1 Blacklisted JTI returns isBlacklisted = true")
    void testBlacklistToken() {
        String jti = "test-jti-" + UUID.randomUUID();
        blacklistService.blacklist(jti, Instant.now().plusSeconds(60));

        assertThat(blacklistService.isBlacklisted(jti)).isTrue();
    }

    @Test
    @DisplayName("7.2 Non-blacklisted JTI returns false")
    void testNonBlacklistedToken() {
        String jti = "non-blacklisted-" + UUID.randomUUID();
        assertThat(blacklistService.isBlacklisted(jti)).isFalse();
    }

    @Test
    @DisplayName("7.3 Blacklist with short TTL expires")
    void testBlacklistWithTtl() throws InterruptedException {
        String jti = "ttl-test-" + UUID.randomUUID();
        blacklistService.blacklist(jti, Instant.now().plusSeconds(2));

        assertThat(blacklistService.isBlacklisted(jti)).isTrue();

        Thread.sleep(3000);

        assertThat(blacklistService.isBlacklisted(jti)).isFalse();
    }

    @Test
    @DisplayName("7.4 Logout blacklists refresh token")
    @Transactional
    void testLogoutBlacklistsRefreshToken() {
        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        TokenResponse response = authService.login(request, "127.0.0.1", "JUnit");

        String refreshToken = response.refreshToken();
        Jwt refreshJwt = jwtDecoder.decode(refreshToken);

        authService.logout(refreshToken);

        assertThat(blacklistService.isBlacklisted(refreshJwt.getId())).isTrue();
    }

    @Test
    @DisplayName("7.5 Blacklist with expired token is ignored (no write)")
    void testBlacklistExpiredTokenIgnored() {
        String jti = "expired-" + UUID.randomUUID();
        blacklistService.blacklist(jti, Instant.now().minusSeconds(60));

        assertThat(blacklistService.isBlacklisted(jti)).isFalse();
    }

    @Test
    @DisplayName("7.6 Logout with null refresh token does not throw")
    @Transactional
    void testLogoutWithNullRefresh() {
        authService.logout(null);
    }
}
