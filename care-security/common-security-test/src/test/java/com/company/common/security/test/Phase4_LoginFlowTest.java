package com.company.common.security.test;

import com.company.common.security.captcha.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.entity.SaUser;
import com.company.common.security.repository.LoginHistoryRepository;
import com.company.common.security.repository.SaUserRepository;
import com.company.common.security.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.transaction.annotation.Transactional;

import static com.company.common.security.test.TestLoginHelper.loginRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@CareSecurityTest
@DisplayName("Phase 4: Login Flow - Authentication with Real DB")
class Phase4_LoginFlowTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private SaUserRepository saUserRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Value("${care-test.admin-password}")
    private String adminPassword;

    @Test
    @DisplayName("4.1 Login success returns TokenResponse")
    @Transactional
    void testLoginSuccess() {
        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        TokenResponse response = authService.login(request, "127.0.0.1", "JUnit");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("4.2 Login success contains roles")
    @Transactional
    void testLoginSuccessContainsRoles() {
        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        TokenResponse response = authService.login(request, "127.0.0.1", "JUnit");

        assertThat(response.roles()).isNotEmpty();
        assertThat(response.roles()).contains("ROLE_ADMIN");
        assertThat(response.username()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("4.3 Login fail with wrong password throws BadCredentialsException")
    @Transactional
    void testLoginFailWrongPassword() {
        LoginRequest request = loginRequest("ADMIN", "WrongPassword!", captchaService);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("4.4 Login fail with non-existent user throws BadCredentialsException")
    @Transactional
    void testLoginFailNonExistentUser() {
        LoginRequest request = loginRequest("NON_EXISTENT_USER_99999", "password", captchaService);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("4.5 Login fail count increments on wrong password")
    @Transactional
    void testLoginFailCountIncrement() {
        SaUser adminBefore = saUserRepository.findByUsername("ADMIN").orElseThrow();
        int initialCount = adminBefore.getLoginFailCount() != null ? adminBefore.getLoginFailCount() : 0;

        LoginRequest request = loginRequest("ADMIN", "WrongPassword!", captchaService);
        try {
            authService.login(request, "127.0.0.1", "JUnit");
        } catch (BadCredentialsException ignored) {}

        SaUser adminAfter = saUserRepository.findByUsername("ADMIN").orElseThrow();
        assertThat(adminAfter.getLoginFailCount()).isGreaterThan(initialCount);
    }

    @Test
    @DisplayName("4.6 Account lock after max attempts (simulated)")
    @Transactional
    void testAccountLockAfterMaxAttempts() {
        SaUser admin = saUserRepository.findByUsername("ADMIN").orElseThrow();
        admin.setAccountLocked(true);
        admin.setLockTime(java.time.LocalDateTime.now());
        saUserRepository.save(admin);
        saUserRepository.flush();

        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("4.7 Locked account cannot login even with correct password")
    @Transactional
    void testLockedAccountCannotLogin() {
        SaUser admin = saUserRepository.findByUsername("ADMIN").orElseThrow();
        admin.setAccountLocked(true);
        admin.setLockTime(java.time.LocalDateTime.now());
        saUserRepository.save(admin);
        saUserRepository.flush();

        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("4.8 Successful login resets fail count")
    @Transactional
    void testSuccessLoginResetsFailCount() {
        SaUser admin = saUserRepository.findByUsername("ADMIN").orElseThrow();
        admin.setLoginFailCount(3);
        saUserRepository.save(admin);
        saUserRepository.flush();

        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        authService.login(request, "127.0.0.1", "JUnit");

        SaUser updated = saUserRepository.findByUsername("ADMIN").orElseThrow();
        assertThat(updated.getLoginFailCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("4.9 Login creates LoginHistory record (success)")
    @Transactional
    void testLoginAuditLogCreated() {
        long countBefore = loginHistoryRepository.count();

        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        authService.login(request, "127.0.0.1", "JUnit");

        long countAfter = loginHistoryRepository.count();
        assertThat(countAfter).isGreaterThan(countBefore);
    }

    @Test
    @DisplayName("4.10 Failed login creates LoginHistory record")
    @Transactional
    void testLoginAuditLogOnFailure() {
        long countBefore = loginHistoryRepository.count();

        LoginRequest request = loginRequest("ADMIN", "WrongPassword!", captchaService);
        try {
            authService.login(request, "127.0.0.1", "JUnit");
        } catch (BadCredentialsException ignored) {}

        long countAfter = loginHistoryRepository.count();
        assertThat(countAfter).isGreaterThan(countBefore);
    }
}
