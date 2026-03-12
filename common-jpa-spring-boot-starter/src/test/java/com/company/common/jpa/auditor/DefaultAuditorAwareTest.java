package com.company.common.jpa.auditor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultAuditorAware 預設審計人提供者驗證
 *
 * 測試在有/無 Spring Security 認證資訊時的行為。
 * 注意：因為 classpath 有 spring-security-core，SECURITY_PRESENT 為 true，
 * 所以無認證資訊時 SecurityAuditorHelper 回傳 Optional.empty()（而非 "SYSTEM"）。
 * "SYSTEM" 只在 classpath 無 Spring Security 或 SecurityAuditorHelper 拋異常時才會回傳。
 */
@DisplayName("DefaultAuditorAware 預設審計人提供者")
class DefaultAuditorAwareTest {

    private final DefaultAuditorAware auditorAware = new DefaultAuditorAware();

    @Test
    @DisplayName("無認證資訊時回傳 empty（classpath 有 Security 但 SecurityContext 為空）")
    void 無認證資訊時回傳empty() {
        SecurityContextHolder.clearContext();

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // SecurityAuditorHelper.getAuditor() 回傳 empty（authentication == null）
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("已認證用戶回傳用戶名稱")
    void 已認證用戶回傳用戶名稱() {
        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("bob", "password", List.of()));

            Optional<String> auditor = auditorAware.getCurrentAuditor();

            assertThat(auditor).isPresent().hasValue("bob");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("匿名用戶（anonymousUser）回傳 empty")
    void 匿名用戶回傳empty() {
        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

            Optional<String> auditor = auditorAware.getCurrentAuditor();

            // principal 為 "anonymousUser" 被明確排除
            assertThat(auditor).isEmpty();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("認證對象未認證時回傳 empty")
    void 未認證時回傳empty() {
        try {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken("user", "pass");
            token.setAuthenticated(false);
            SecurityContextHolder.getContext().setAuthentication(token);

            Optional<String> auditor = auditorAware.getCurrentAuditor();

            // isAuthenticated() == false
            assertThat(auditor).isEmpty();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("不同用戶切換時回傳對應的用戶名稱")
    void 不同用戶切換() {
        try {
            // 設定用戶 A
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("alice", "pass", List.of()));
            assertThat(auditorAware.getCurrentAuditor()).hasValue("alice");

            // 切換為用戶 B
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("bob", "pass", List.of()));
            assertThat(auditorAware.getCurrentAuditor()).hasValue("bob");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
