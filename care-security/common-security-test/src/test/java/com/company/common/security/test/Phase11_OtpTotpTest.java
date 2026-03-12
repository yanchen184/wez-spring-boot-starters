package com.company.common.security.test;

import com.company.common.security.captcha.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;
import com.company.common.security.dto.request.OtpLoginRequest;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.entity.SaUser;
import com.company.common.security.otp.OtpService;
import com.company.common.security.otp.OtpService.OtpSetupResult;
import com.company.common.security.otp.TotpService;
import com.company.common.security.repository.SaUserRepository;
import com.company.common.security.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static com.company.common.security.test.TestLoginHelper.loginRequest;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 11: TOTP Two-Factor Authentication
 *
 * === TDD Guide for Engineers ===
 *
 * This test file serves as the SPECIFICATION before writing any implementation code.
 * Each test describes a business requirement. Follow this pattern for future features:
 *
 * 1. Read the test names first — they define WHAT the system should do
 * 2. Run all tests → they should all FAIL (Red)
 * 3. Implement the minimum code to make each test pass (Green)
 * 4. Refactor while keeping tests green (Refactor)
 *
 * Test structure follows business scenarios, NOT class structure:
 *
 * 11.1 TOTP Algorithm Specification
 *     → What: RFC 6238 TOTP code generation and verification rules
 *     → Why: Foundation for all OTP features, must be correct before building anything on top
 *
 * 11.2 OTP Setup Flow
 *     → What: User enables OTP on their account
 *     → Why: User story — "As a user, I want to set up 2FA to secure my account"
 *
 * 11.3 OTP Login Flow
 *     → What: Login behavior changes when OTP is enabled
 *     → Why: User story — "As a user with OTP enabled, I must verify my identity with a second factor"
 *
 * 11.4 OTP Disable Flow
 *     → What: User disables OTP
 *     → Why: User story — "As a user, I want to turn off 2FA if I no longer need it"
 *
 * 11.5 HTTP API Contract
 *     → What: REST endpoints behave correctly, auth rules enforced
 *     → Why: Contract between frontend and backend, must be stable
 *
 * === Implementation Checklist (derived from tests) ===
 *
 * [ ] TotpService — RFC 6238 TOTP with Base32, 6 digits, 30s step, HmacSHA1
 * [ ] OtpService  — setup(secret generation + DB), verify-setup(enable), verify(login), disable
 * [ ] AuthService — login() checks otpEnabled, returns requiresOtp; completeOtpLogin() issues tokens
 * [ ] TokenResponse — add requiresOtp field, otpRequired() factory, backward-compatible constructors
 * [ ] OtpController — REST endpoints: /setup, /verify-setup, /verify (public), DELETE /
 * [ ] SecurityConfig — /api/auth/otp/verify must be permitAll
 * [ ] SaUser entity — OTP_SECRET (varchar 64), OTP_ENABLED (bit) columns
 */
@CareSecurityTest
@AutoConfigureMockMvc
@Import({com.company.common.security.otp.OtpController.class,
         com.company.common.security.controller.AuthController.class,
         com.company.common.security.exception.SecurityExceptionHandler.class})
@DisplayName("Phase 11: TOTP Two-Factor Authentication")
class Phase11_OtpTotpTest {

    @Autowired private TotpService totpService;
    @Autowired private OtpService otpService;
    @Autowired private AuthService authService;
    @Autowired private CaptchaService captchaService;
    @Autowired private SaUserRepository saUserRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Value("${care-test.admin-password}")
    private String adminPassword;

    // =====================================================================
    // 11.1 TOTP Algorithm Specification
    //
    // Requirement: Implement RFC 6238 TOTP algorithm
    //   - Secret: 20 bytes, Base32 encoded (32 chars)
    //   - Code: 6-digit numeric, 30-second time window
    //   - Clock skew tolerance: configurable (default: +/- 1 step = 30 seconds)
    //   - OTP Auth URI: otpauth://totp/{issuer}:{username}?secret=...
    //
    // Write TotpService to satisfy these tests.
    // =====================================================================

    @Nested
    @DisplayName("11.1 TOTP Algorithm")
    class TotpAlgorithm {

        @Test
        @DisplayName("Secret is 32-char Base32 string (20 bytes)")
        void secretFormat() {
            String secret = totpService.generateSecret();
            assertThat(secret)
                    .hasSize(32)
                    .matches("[A-Z2-7]+");
        }

        @Test
        @DisplayName("Each generated secret is unique")
        void secretUniqueness() {
            String s1 = totpService.generateSecret();
            String s2 = totpService.generateSecret();
            assertThat(s1).isNotEqualTo(s2);
        }

        @Test
        @DisplayName("Code is 6-digit numeric string")
        void codeFormat() {
            String code = totpService.generateCode(totpService.generateSecret());
            assertThat(code)
                    .hasSize(6)
                    .matches("\\d{6}");
        }

        @Test
        @DisplayName("Same secret + same time = same code (deterministic)")
        void codeDeterministic() {
            String secret = totpService.generateSecret();
            Instant t = Instant.ofEpochSecond(1700000000L);
            assertThat(totpService.generateCode(secret, t))
                    .isEqualTo(totpService.generateCode(secret, t));
        }

        @Test
        @DisplayName("Correct code is accepted")
        void verifyCorrectCode() {
            String secret = totpService.generateSecret();
            String code = totpService.generateCode(secret);
            assertThat(totpService.verifyCode(secret, code)).isTrue();
        }

        @Test
        @DisplayName("Incorrect code is rejected")
        void verifyIncorrectCode() {
            String secret = totpService.generateSecret();
            assertThat(totpService.verifyCode(secret, "000000")).isFalse();
        }

        @Test
        @DisplayName("Code from previous time step is accepted (skew tolerance)")
        void verifyWithClockSkew() {
            String secret = totpService.generateSecret();
            Instant now = Instant.now();
            String pastCode = totpService.generateCode(secret, now.minusSeconds(30));
            assertThat(totpService.verifyCode(secret, pastCode, now)).isTrue();
        }

        @Test
        @DisplayName("Code from 3 steps ago is rejected (beyond skew)")
        void verifyBeyondClockSkew() {
            String secret = totpService.generateSecret();
            Instant now = Instant.now();
            String oldCode = totpService.generateCode(secret, now.minusSeconds(90));
            assertThat(totpService.verifyCode(secret, oldCode, now)).isFalse();
        }

        @Test
        @DisplayName("OTP Auth URI contains required parameters")
        void otpAuthUri() {
            String secret = "JBSWY3DPEHPK3PXP";
            String uri = totpService.buildOtpAuthUri(secret, "testuser", "MyApp");

            assertThat(uri).startsWith("otpauth://totp/");
            assertThat(uri).contains("secret=" + secret);
            assertThat(uri).contains("issuer=MyApp");
            assertThat(uri).contains("digits=6");
            assertThat(uri).contains("period=30");
        }

        @Test
        @DisplayName("Base32 encode → decode roundtrip preserves data")
        void base32Roundtrip() {
            String secret = totpService.generateSecret();
            byte[] decoded = TotpService.Base32.decode(secret);
            assertThat(TotpService.Base32.encode(decoded)).isEqualTo(secret);
        }
    }

    // =====================================================================
    // 11.2 OTP Setup Flow
    //
    // User Story: As a user, I want to set up 2FA on my account
    //
    // Acceptance Criteria:
    //   1. Call setupOtp → receive secret + QR code URI
    //   2. OTP is NOT yet active (two-phase: setup then verify)
    //   3. Secret is persisted in the user record
    //   4. User scans QR code, enters code from authenticator app
    //   5. verifyAndEnableOtp with correct code → OTP becomes active
    //   6. verifyAndEnableOtp with wrong code → OTP stays inactive
    //   7. Re-calling setupOtp generates a new secret (overwrite)
    //
    // Write OtpService.setupOtp() and OtpService.verifyAndEnableOtp()
    // to satisfy these tests.
    // =====================================================================

    @Nested
    @DisplayName("11.2 OTP Setup Flow")
    class OtpSetupFlow {

        @Test
        @DisplayName("Setup returns secret and QR URI")
        @Transactional
        void setupReturnsSecretAndUri() {
            OtpSetupResult result = otpService.setupOtp("ADMIN");
            assertThat(result.secret()).matches("[A-Z2-7]{32}");
            assertThat(result.otpAuthUri()).startsWith("otpauth://totp/");
        }

        @Test
        @DisplayName("After setup, OTP is NOT yet enabled (two-phase)")
        @Transactional
        void setupDoesNotEnableOtp() {
            otpService.setupOtp("ADMIN");
            assertThat(otpService.isOtpEnabled("ADMIN")).isFalse();
        }

        @Test
        @DisplayName("Setup persists secret in user record")
        @Transactional
        void setupPersistsSecret() {
            OtpSetupResult result = otpService.setupOtp("ADMIN");
            SaUser user = saUserRepository.findByUsername("ADMIN").orElseThrow();
            assertThat(user.getOtpSecret()).isEqualTo(result.secret());
            assertThat(user.getOtpEnabled()).isFalse();
        }

        @Test
        @DisplayName("Verify with correct code activates OTP")
        @Transactional
        void verifyCorrectCodeActivates() {
            OtpSetupResult result = otpService.setupOtp("ADMIN");
            String code = totpService.generateCode(result.secret());

            assertThat(otpService.verifyAndEnableOtp("ADMIN", code)).isTrue();
            assertThat(otpService.isOtpEnabled("ADMIN")).isTrue();
        }

        @Test
        @DisplayName("Verify with wrong code does NOT activate OTP")
        @Transactional
        void verifyWrongCodeDoesNotActivate() {
            otpService.setupOtp("ADMIN");

            assertThat(otpService.verifyAndEnableOtp("ADMIN", "000000")).isFalse();
            assertThat(otpService.isOtpEnabled("ADMIN")).isFalse();
        }

        @Test
        @DisplayName("Re-setup generates new secret, overwrites previous")
        @Transactional
        void reSetupOverwritesPrevious() {
            OtpSetupResult first = otpService.setupOtp("ADMIN");
            OtpSetupResult second = otpService.setupOtp("ADMIN");
            assertThat(second.secret()).isNotEqualTo(first.secret());

            SaUser user = saUserRepository.findByUsername("ADMIN").orElseThrow();
            assertThat(user.getOtpSecret()).isEqualTo(second.secret());
        }

        @Test
        @DisplayName("Setup for non-existent user throws exception")
        void setupNonExistentUser() {
            assertThatThrownBy(() -> otpService.setupOtp("GHOST"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Verify without prior setup throws exception")
        @Transactional
        void verifyWithoutSetup() {
            // Ensure no secret
            SaUser user = saUserRepository.findByUsername("ADMIN").orElseThrow();
            user.setOtpSecret(null);
            user.setOtpEnabled(false);
            saUserRepository.save(user);

            assertThatThrownBy(() -> otpService.verifyAndEnableOtp("ADMIN", "123456"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OTP not set up");
        }
    }

    // =====================================================================
    // 11.3 OTP Login Flow
    //
    // User Story: As a user with OTP enabled, I must enter a TOTP code
    //             after my password to complete login.
    //
    // Acceptance Criteria:
    //   1. User without OTP → login returns tokens directly (no change)
    //   2. User with OTP → login returns requiresOtp=true, NO tokens
    //   3. Frontend collects OTP code, calls verify endpoint
    //   4. Correct OTP code → completeOtpLogin issues JWT tokens
    //   5. Wrong OTP code → rejected, no tokens issued
    //   6. verifyOtp returns false if OTP not enabled (edge case)
    //
    // Write AuthService.login() OTP check and AuthService.completeOtpLogin()
    // to satisfy these tests.
    // =====================================================================

    @Nested
    @DisplayName("11.3 OTP Login Flow")
    class OtpLoginFlow {

        @Test
        @DisplayName("User without OTP: login returns tokens directly")
        @Transactional
        void loginWithoutOtp() {
            TokenResponse resp = authService.login(
                    loginRequest("ADMIN", adminPassword, captchaService), "127.0.0.1", "JUnit");

            assertThat(resp.requiresOtp()).isFalse();
            assertThat(resp.accessToken()).isNotNull();
            assertThat(resp.refreshToken()).isNotNull();
        }

        @Test
        @DisplayName("User with OTP: login returns requiresOtp and null tokens")
        @Transactional
        void loginWithOtpReturnsChallenge() {
            enableOtpFor("ADMIN");

            TokenResponse resp = authService.login(
                    loginRequest("ADMIN", adminPassword, captchaService), "127.0.0.1", "JUnit");

            assertThat(resp.requiresOtp()).isTrue();
            assertThat(resp.accessToken()).isNull();
            assertThat(resp.refreshToken()).isNull();
            assertThat(resp.username()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Correct OTP code passes verification")
        @Transactional
        void verifyCorrectOtpCode() {
            String secret = enableOtpFor("ADMIN");
            String code = totpService.generateCode(secret);

            assertThat(otpService.verifyOtp("ADMIN", code)).isTrue();
        }

        @Test
        @DisplayName("Wrong OTP code fails verification")
        @Transactional
        void verifyWrongOtpCode() {
            enableOtpFor("ADMIN");
            assertThat(otpService.verifyOtp("ADMIN", "000000")).isFalse();
        }

        @Test
        @DisplayName("verifyOtp returns false when OTP not enabled")
        @Transactional
        void verifyOtpWhenNotEnabled() {
            // Only setup, don't enable
            otpService.setupOtp("ADMIN");
            assertThat(otpService.verifyOtp("ADMIN", "123456")).isFalse();
        }

        @Test
        @DisplayName("completeOtpLogin issues valid JWT tokens")
        @Transactional
        void completeOtpLoginIssuesTokens() {
            TokenResponse resp = authService.completeOtpLogin("ADMIN", "127.0.0.1", "JUnit");

            assertThat(resp.accessToken()).isNotNull();
            assertThat(resp.refreshToken()).isNotNull();
            assertThat(resp.username()).isEqualTo("ADMIN");
            assertThat(resp.requiresOtp()).isFalse();
        }

        @Test
        @DisplayName("Full flow: login → challenge → verify → tokens")
        @Transactional
        void fullOtpLoginFlow() {
            // 1. Enable OTP
            String secret = enableOtpFor("ADMIN");

            // 2. Login → requiresOtp
            TokenResponse loginResp = authService.login(
                    loginRequest("ADMIN", adminPassword, captchaService), "127.0.0.1", "JUnit");
            assertThat(loginResp.requiresOtp()).isTrue();

            // 3. Verify OTP
            String code = totpService.generateCode(secret);
            assertThat(otpService.verifyOtp("ADMIN", code)).isTrue();

            // 4. Complete login → tokens
            TokenResponse finalResp = authService.completeOtpLogin("ADMIN", "127.0.0.1", "JUnit");
            assertThat(finalResp.accessToken()).isNotNull();
            assertThat(finalResp.requiresOtp()).isFalse();
        }
    }

    // =====================================================================
    // 11.4 OTP Disable Flow
    //
    // User Story: As a user, I want to turn off 2FA if I no longer need it
    //
    // Acceptance Criteria:
    //   1. disableOtp removes secret from DB
    //   2. After disable, isOtpEnabled returns false
    //   3. After disable, login no longer requires OTP
    // =====================================================================

    @Nested
    @DisplayName("11.4 OTP Disable Flow")
    class OtpDisableFlow {

        @Test
        @DisplayName("Disable removes secret and sets otpEnabled=false")
        @Transactional
        void disableRemovesSecret() {
            enableOtpFor("ADMIN");
            assertThat(otpService.isOtpEnabled("ADMIN")).isTrue();

            otpService.disableOtp("ADMIN");

            assertThat(otpService.isOtpEnabled("ADMIN")).isFalse();
            SaUser user = saUserRepository.findByUsername("ADMIN").orElseThrow();
            assertThat(user.getOtpSecret()).isNull();
        }

        @Test
        @DisplayName("After disable, login returns tokens directly (no OTP challenge)")
        @Transactional
        void loginAfterDisable() {
            enableOtpFor("ADMIN");
            otpService.disableOtp("ADMIN");

            TokenResponse resp = authService.login(
                    loginRequest("ADMIN", adminPassword, captchaService), "127.0.0.1", "JUnit");

            assertThat(resp.requiresOtp()).isFalse();
            assertThat(resp.accessToken()).isNotNull();
        }
    }

    // =====================================================================
    // 11.5 TokenResponse Contract
    //
    // Requirement: TokenResponse must support OTP challenge without
    //              breaking existing consumers (backward compatible).
    //
    //   1. otpRequired() factory → requiresOtp=true, null tokens
    //   2. Existing constructors → requiresOtp defaults to false
    // =====================================================================

    @Nested
    @DisplayName("11.5 TokenResponse Contract")
    class TokenResponseContract {

        @Test
        @DisplayName("otpRequired() returns challenge response with null tokens")
        void otpRequiredFactory() {
            TokenResponse resp = TokenResponse.otpRequired("USER1");

            assertThat(resp.requiresOtp()).isTrue();
            assertThat(resp.accessToken()).isNull();
            assertThat(resp.refreshToken()).isNull();
            assertThat(resp.tokenType()).isNull();
            assertThat(resp.expiresIn()).isZero();
            assertThat(resp.username()).isEqualTo("USER1");
            assertThat(resp.roles()).isEmpty();
        }

        @Test
        @DisplayName("Existing 8-param constructor defaults requiresOtp=false")
        void backwardCompatible8Params() {
            TokenResponse resp = new TokenResponse(
                    "at", "rt", "Bearer", 1800, "USER", List.of("ROLE_USER"), null, null);
            assertThat(resp.requiresOtp()).isFalse();
        }

        @Test
        @DisplayName("Existing 9-param constructor defaults requiresOtp=false")
        void backwardCompatible9Params() {
            TokenResponse resp = new TokenResponse(
                    "at", "rt", "Bearer", 1800, "USER", List.of("ROLE_USER"), null, null, "LOCAL");
            assertThat(resp.requiresOtp()).isFalse();
        }
    }

    // =====================================================================
    // 11.6 HTTP API Contract
    //
    // Requirement: REST endpoints for OTP management
    //
    // Endpoints:
    //   POST   /api/auth/otp/setup        (authenticated) → secret + URI
    //   POST   /api/auth/otp/verify-setup  (authenticated) → enable OTP
    //   POST   /api/auth/otp/verify        (PUBLIC)        → verify OTP during login → tokens
    //   DELETE /api/auth/otp               (authenticated) → disable OTP
    //
    // Security rules:
    //   - /api/auth/otp/verify is permitAll (called before user has tokens)
    //   - All other endpoints require valid JWT
    // =====================================================================

    @Nested
    @DisplayName("11.6 HTTP API Contract")
    class HttpApiContract {

        private String adminAccessToken;

        @BeforeEach
        void setUp() {
            TokenResponse resp = authService.login(
                    loginRequest("ADMIN", adminPassword, captchaService), "127.0.0.1", "JUnit");
            adminAccessToken = resp.accessToken();
        }

        // --- Authentication rules ---

        @Test
        @DisplayName("POST /otp/setup without token → 401")
        void setupRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/auth/otp/setup"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /otp/verify-setup without token → 401")
        void verifySetupRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/auth/otp/verify-setup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"123456\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE /otp without token → 401")
        void disableRequiresAuth() throws Exception {
            mockMvc.perform(delete("/api/auth/otp"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /otp/verify is public (no token needed)")
        @Transactional
        void verifyLoginIsPublic() throws Exception {
            String secret = enableOtpFor("ADMIN");
            String code = totpService.generateCode(secret);

            mockMvc.perform(post("/api/auth/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OtpLoginRequest("ADMIN", code))))
                    .andExpect(status().isOk());
        }

        // --- Setup endpoint ---

        @Test
        @DisplayName("POST /otp/setup → 200 with secret and URI")
        @Transactional
        void setupEndpoint() throws Exception {
            mockMvc.perform(post("/api/auth/otp/setup")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.secret").isNotEmpty())
                    .andExpect(jsonPath("$.data.otpAuthUri").isNotEmpty());
        }

        // --- Verify setup endpoint ---

        @Test
        @DisplayName("POST /otp/verify-setup with correct code → 200")
        @Transactional
        void verifySetupCorrectCode() throws Exception {
            OtpSetupResult setup = otpService.setupOtp("ADMIN");
            String code = totpService.generateCode(setup.secret());

            mockMvc.perform(post("/api/auth/otp/verify-setup")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"" + code + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST /otp/verify-setup with wrong code → 400")
        @Transactional
        void verifySetupWrongCode() throws Exception {
            otpService.setupOtp("ADMIN");

            mockMvc.perform(post("/api/auth/otp/verify-setup")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"000000\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        // --- Verify login endpoint ---

        @Test
        @DisplayName("POST /otp/verify with correct code → 200 + tokens")
        @Transactional
        void verifyLoginCorrectCode() throws Exception {
            String secret = enableOtpFor("ADMIN");
            String code = totpService.generateCode(secret);

            mockMvc.perform(post("/api/auth/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OtpLoginRequest("ADMIN", code))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("POST /otp/verify with wrong code → 400")
        @Transactional
        void verifyLoginWrongCode() throws Exception {
            enableOtpFor("ADMIN");

            mockMvc.perform(post("/api/auth/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OtpLoginRequest("ADMIN", "000000"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        // --- Disable endpoint ---

        @Test
        @DisplayName("DELETE /otp → 200, OTP disabled")
        @Transactional
        void disableEndpoint() throws Exception {
            enableOtpFor("ADMIN");

            mockMvc.perform(delete("/api/auth/otp")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            assertThat(otpService.isOtpEnabled("ADMIN")).isFalse();
        }

        // --- Full HTTP flow ---

        @Test
        @DisplayName("E2E: login → requiresOtp → POST /otp/verify → tokens")
        @Transactional
        void fullHttpFlow() throws Exception {
            String secret = enableOtpFor("ADMIN");

            // 1. Login → requiresOtp
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    loginRequest("ADMIN", adminPassword, captchaService))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.requiresOtp").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isEmpty());

            // 2. Verify OTP → get tokens
            String code = totpService.generateCode(secret);
            mockMvc.perform(post("/api/auth/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OtpLoginRequest("ADMIN", code))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.requiresOtp").value(false));
        }
    }

    // =====================================================================
    // Helper: enable OTP for a user (setup + verify in one step)
    // Returns the secret for generating codes in tests.
    // =====================================================================

    private String enableOtpFor(String username) {
        OtpSetupResult setup = otpService.setupOtp(username);
        String code = totpService.generateCode(setup.secret());
        otpService.verifyAndEnableOtp(username, code);
        return setup.secret();
    }
}
