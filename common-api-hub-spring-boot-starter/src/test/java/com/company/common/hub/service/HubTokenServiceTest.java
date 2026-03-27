package com.company.common.hub.service;

import com.company.common.hub.exception.HubTokenException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HubTokenService 單元測試（純邏輯，不需 Spring Context）。
 */
@DisplayName("HubTokenService 測試")
class HubTokenServiceTest {

    /** 測試用密鑰（至少 32 字元）。 */
    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars-long!!";

    private HubTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new HubTokenService(SECRET);
    }

    @Nested
    @DisplayName("Token 簽發")
    class IssueToken {

        @Test
        @DisplayName("應簽發包含正確 Claims 的 Token")
        void shouldIssueTokenWithCorrectClaims_whenValidInput() {
            String token = tokenService.issueToken(1L, 100L, 10L, 3600);

            assertThat(token).isNotBlank();

            Claims claims = tokenService.validateToken(token);
            assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
            assertThat(claims.get("orgId", Long.class)).isEqualTo(100L);
            assertThat(claims.get("hubSetId", Long.class)).isEqualTo(10L);
            assertThat(claims.getSubject()).isEqualTo("1");
        }

        @Test
        @DisplayName("orgId 為 null 時 Token 不應包含 orgId claim")
        void shouldNotIncludeOrgId_whenOrgIdIsNull() {
            String token = tokenService.issueToken(1L, null, 10L, 3600);

            Claims claims = tokenService.validateToken(token);
            assertThat(claims.get("orgId")).isNull();
        }

        @Test
        @DisplayName("Token 應設定正確的過期時間")
        void shouldSetExpiration_whenAgingProvided() {
            String token = tokenService.issueToken(1L, null, 10L, 3600);

            Claims claims = tokenService.validateToken(token);
            assertThat(claims.getExpiration()).isNotNull();
            assertThat(claims.getIssuedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Token 驗證")
    class ValidateToken {

        @Test
        @DisplayName("合法 Token 應解出正確 Claims")
        void shouldReturnClaims_whenTokenValid() {
            String token = tokenService.issueToken(42L, 7L, 3L, 3600);

            Claims claims = tokenService.validateToken(token);

            assertThat(claims.get("userId", Long.class)).isEqualTo(42L);
            assertThat(claims.get("orgId", Long.class)).isEqualTo(7L);
            assertThat(claims.get("hubSetId", Long.class)).isEqualTo(3L);
        }

        @Test
        @DisplayName("過期 Token 應拋出 HubTokenException(422002)")
        void shouldThrowExpired_whenTokenExpired() {
            String token = tokenService.issueToken(1L, null, 10L, -1);

            assertThatThrownBy(() -> tokenService.validateToken(token))
                    .isInstanceOf(HubTokenException.class)
                    .satisfies(ex -> {
                        HubTokenException hubEx = (HubTokenException) ex;
                        assertThat(hubEx.getCode()).isEqualTo("422002");
                    });
        }

        @Test
        @DisplayName("被竄改的 Token 應拋出 HubTokenException(422001)")
        void shouldThrowInvalid_whenTokenTampered() {
            String token = tokenService.issueToken(1L, null, 10L, 3600);
            String tamperedToken = token + "tampered";

            assertThatThrownBy(() -> tokenService.validateToken(tamperedToken))
                    .isInstanceOf(HubTokenException.class)
                    .satisfies(ex -> {
                        HubTokenException hubEx = (HubTokenException) ex;
                        assertThat(hubEx.getCode()).isEqualTo("422001");
                    });
        }

        @Test
        @DisplayName("不同密鑰簽的 Token 應拋出 HubTokenException(422001)")
        void shouldThrowInvalid_whenSignedWithDifferentKey() {
            HubTokenService otherService = new HubTokenService(
                    "another-secret-key-that-is-also-32-chars-long!!"
            );
            String token = otherService.issueToken(1L, null, 10L, 3600);

            assertThatThrownBy(() -> tokenService.validateToken(token))
                    .isInstanceOf(HubTokenException.class)
                    .satisfies(ex -> {
                        HubTokenException hubEx = (HubTokenException) ex;
                        assertThat(hubEx.getCode()).isEqualTo("422001");
                    });
        }
    }

    @Nested
    @DisplayName("Token 黑名單")
    class Blacklist {

        @Test
        @DisplayName("未加入黑名單的 Token 應回傳 false")
        void shouldReturnFalse_whenTokenNotBlacklisted() {
            String token = tokenService.issueToken(1L, null, 10L, 3600);

            assertThat(tokenService.isBlacklisted(token)).isFalse();
        }

        @Test
        @DisplayName("加入黑名單後應回傳 true")
        void shouldReturnTrue_whenTokenBlacklisted() {
            String token = tokenService.issueToken(1L, null, 10L, 3600);

            tokenService.blacklistToken(token);

            assertThat(tokenService.isBlacklisted(token)).isTrue();
        }

        @Test
        @DisplayName("黑名單只影響特定 Token")
        void shouldOnlyAffectSpecificToken_whenBlacklisted() {
            String token1 = tokenService.issueToken(1L, null, 10L, 3600);
            String token2 = tokenService.issueToken(2L, null, 10L, 3600);

            tokenService.blacklistToken(token1);

            assertThat(tokenService.isBlacklisted(token1)).isTrue();
            assertThat(tokenService.isBlacklisted(token2)).isFalse();
        }
    }
}
