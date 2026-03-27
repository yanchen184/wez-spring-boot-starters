package com.company.common.security.test;

import com.company.common.security.autoconfigure.CareSecurityProperties;
import com.company.common.security.service.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 12: Image CAPTCHA (Verification Code)
 *
 * === TDD Guide for Engineers ===
 *
 * This test defines the CAPTCHA feature specification.
 *
 * 12.1 CAPTCHA Generation
 *     -> What: System generates a random numeric CAPTCHA image
 *     -> Why: "As a system, I want to prevent automated brute-force login attempts"
 *
 * 12.2 CAPTCHA Verification
 *     -> What: Login requires valid CAPTCHA when enabled
 *     -> Why: "As a user, I must enter the correct verification code to log in"
 *
 * 12.3 CAPTCHA Error Messages
 *     -> What: Clear error messages for invalid/expired/missing CAPTCHA
 *     -> Why: "As a user, I want to know why my login failed"
 *
 * 12.4 HTTP API Contract
 *     -> What: REST endpoints for CAPTCHA generation and login with CAPTCHA
 *     -> Why: Contract between frontend and backend
 *
 * === Implementation Checklist ===
 *
 * [ ] CaptchaService     -- generate(4-6 digit code + image), verify(id + answer), store in Redis (TTL 5 min)
 * [ ] CaptchaController  -- GET /api/auth/captcha returns { captchaId, image(base64) }
 * [ ] CaptchaResponse    -- DTO: captchaId + image
 * [ ] LoginRequest       -- add optional captchaId + captchaAnswer fields
 * [ ] AuthService.login  -- validate CAPTCHA before password check (when enabled)
 * [ ] SecurityConfig     -- /api/auth/captcha must be permitAll
 * [ ] CareSecurityProperties.Captcha -- enabled, length, expireSeconds
 * [ ] CareSecurityAutoConfiguration  -- ConditionalOnProperty for captcha beans
 */
@CareSecurityTest
@AutoConfigureMockMvc
@Import({com.company.common.security.controller.CaptchaController.class,
         com.company.common.security.controller.AuthController.class,
         com.company.common.security.exception.SecurityExceptionHandler.class})
@DisplayName("Phase 12: Image CAPTCHA")
class Phase12_CaptchaTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CaptchaService captchaService;
    @Autowired RedisTemplate<String, Object> redisTemplate;

    /** Read captcha answer directly from Redis (test-only replacement for removed getAnswerForTest). */
    private String getCaptchaAnswer(String captchaId) {
        Object stored = redisTemplate.opsForValue().get("captcha:" + captchaId);
        return stored != null ? stored.toString() : null;
    }

    // ========================================================================
    // 12.1 CAPTCHA Generation
    //
    // User Story: As a system, I generate numeric image CAPTCHAs stored in Redis.
    //
    // Acceptance Criteria:
    //   - generateCaptcha() returns a CaptchaResult with id and base64 image
    //   - The code is numeric and has configurable length
    //   - Each call generates a unique id
    //   - The image is a valid PNG in base64 format
    // ========================================================================

    @Nested
    @DisplayName("12.1 CAPTCHA Generation")
    class CaptchaGeneration {

        @Test
        @DisplayName("generateCaptcha returns id and base64 image")
        void generateCaptcha_returnsIdAndImage() {
            CaptchaService.CaptchaResult result = captchaService.generateCaptcha();

            assertThat(result).isNotNull();
            assertThat(result.captchaId()).isNotBlank();
            assertThat(result.imageBase64()).isNotBlank();
        }

        @Test
        @DisplayName("each generation produces unique id")
        void generateCaptcha_uniqueIds() {
            CaptchaService.CaptchaResult r1 = captchaService.generateCaptcha();
            CaptchaService.CaptchaResult r2 = captchaService.generateCaptcha();

            assertThat(r1.captchaId()).isNotEqualTo(r2.captchaId());
        }

        @Test
        @DisplayName("image is valid base64 PNG")
        void generateCaptcha_validBase64Png() {
            CaptchaService.CaptchaResult result = captchaService.generateCaptcha();

            // base64 decode should not throw
            byte[] imageBytes = java.util.Base64.getDecoder().decode(result.imageBase64());
            // PNG magic bytes: 137 80 78 71
            assertThat(imageBytes[0]).isEqualTo((byte) 0x89);
            assertThat(imageBytes[1]).isEqualTo((byte) 0x50); // 'P'
            assertThat(imageBytes[2]).isEqualTo((byte) 0x4E); // 'N'
            assertThat(imageBytes[3]).isEqualTo((byte) 0x47); // 'G'
        }
    }

    // ========================================================================
    // 12.2 CAPTCHA Verification
    //
    // User Story: As a user, I enter the verification code shown in the image.
    //             The system validates it before checking my password.
    //
    // Acceptance Criteria:
    //   - Correct answer -> verification passes
    //   - Wrong answer -> verification fails
    //   - Expired CAPTCHA -> verification fails
    //   - CAPTCHA is single-use (cannot reuse same id)
    //   - Case insensitive (if alphanumeric in future)
    // ========================================================================

    @Nested
    @DisplayName("12.2 CAPTCHA Verification")
    class CaptchaVerification {

        @Test
        @DisplayName("correct answer passes verification")
        void verify_correctAnswer_passes() {
            CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
            // Use internal method to get the answer for testing
            String answer = getCaptchaAnswer(result.captchaId());

            assertThat(captchaService.verifyCaptcha(result.captchaId(), answer)).isTrue();
        }

        @Test
        @DisplayName("wrong answer fails verification")
        void verify_wrongAnswer_fails() {
            CaptchaService.CaptchaResult result = captchaService.generateCaptcha();

            assertThat(captchaService.verifyCaptcha(result.captchaId(), "000000")).isFalse();
        }

        @Test
        @DisplayName("non-existent captcha id fails")
        void verify_invalidId_fails() {
            assertThat(captchaService.verifyCaptcha("non-existent-id", "123456")).isFalse();
        }

        @Test
        @DisplayName("captcha is single-use — second verification fails")
        void verify_singleUse() {
            CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
            String answer = getCaptchaAnswer(result.captchaId());

            assertThat(captchaService.verifyCaptcha(result.captchaId(), answer)).isTrue();
            // Second attempt with same id should fail
            assertThat(captchaService.verifyCaptcha(result.captchaId(), answer)).isFalse();
        }
    }

    // ========================================================================
    // 12.3 Login with CAPTCHA
    //
    // User Story: As a user, when CAPTCHA is enabled, I must provide a valid
    //             CAPTCHA code along with my credentials to log in.
    //
    // Acceptance Criteria:
    //   - Login without captchaId/captchaAnswer -> 400 "Captcha is required"
    //   - Login with wrong captcha answer -> 400 "Invalid captcha"
    //   - Login with correct captcha + wrong password -> 401 "Invalid credentials"
    //   - Login with correct captcha + correct password -> 200 success
    // ========================================================================

    @Nested
    @DisplayName("12.3 Login with CAPTCHA")
    class LoginWithCaptcha {

        @Test
        @DisplayName("login without captcha returns 400")
        void login_noCaptcha_returns400() throws Exception {
            String body = """
                    {"username":"ADMIN","password":"Admin@123"}
                    """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Captcha is required"));
        }

        @Test
        @DisplayName("login with wrong captcha returns 400")
        void login_wrongCaptcha_returns400() throws Exception {
            CaptchaService.CaptchaResult captcha = captchaService.generateCaptcha();

            String body = String.format("""
                    {"username":"ADMIN","password":"Admin@123","captchaId":"%s","captchaAnswer":"000000"}
                    """, captcha.captchaId());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid captcha"));
        }

        @Test
        @DisplayName("login with correct captcha + wrong password returns 401")
        void login_correctCaptcha_wrongPassword_returns401() throws Exception {
            CaptchaService.CaptchaResult captcha = captchaService.generateCaptcha();
            String answer = getCaptchaAnswer(captcha.captchaId());

            String body = String.format("""
                    {"username":"ADMIN","password":"WrongPass","captchaId":"%s","captchaAnswer":"%s"}
                    """, captcha.captchaId(), answer);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("login with correct captcha + correct password returns 200")
        void login_correctCaptcha_correctPassword_returns200() throws Exception {
            CaptchaService.CaptchaResult captcha = captchaService.generateCaptcha();
            String answer = getCaptchaAnswer(captcha.captchaId());

            String body = String.format("""
                    {"username":"ADMIN","password":"Admin@123","captchaId":"%s","captchaAnswer":"%s"}
                    """, captcha.captchaId(), answer);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("reused captcha id on login returns 400")
        void login_reusedCaptcha_returns400() throws Exception {
            CaptchaService.CaptchaResult captcha = captchaService.generateCaptcha();
            String answer = getCaptchaAnswer(captcha.captchaId());

            // First login succeeds
            String body = String.format("""
                    {"username":"ADMIN","password":"Admin@123","captchaId":"%s","captchaAnswer":"%s"}
                    """, captcha.captchaId(), answer);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());

            // Second login with same captcha fails
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid captcha"));
        }
    }

    // ========================================================================
    // 12.4 HTTP API Contract
    //
    // User Story: As a frontend developer, I need stable API contracts.
    //
    // Acceptance Criteria:
    //   - GET /api/auth/captcha -> 200 { captchaId, image }
    //   - GET /api/auth/captcha does not require authentication
    //   - Response content type is application/json
    // ========================================================================

    @Nested
    @DisplayName("12.4 HTTP API Contract")
    class HttpApiContract {

        @Test
        @DisplayName("GET /api/auth/captcha returns 200 with captchaId and image")
        void getCaptcha_returns200() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/auth/captcha"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.captchaId").isNotEmpty())
                    .andExpect(jsonPath("$.data.image").isNotEmpty())
                    .andReturn();
        }

        @Test
        @DisplayName("GET /api/auth/captcha does not require authentication")
        void getCaptcha_noAuthRequired() throws Exception {
            // No Authorization header
            mockMvc.perform(get("/api/auth/captcha"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/auth/captcha returns different captcha each time")
        void getCaptcha_differentEachTime() throws Exception {
            MvcResult r1 = mockMvc.perform(get("/api/auth/captcha"))
                    .andReturn();
            MvcResult r2 = mockMvc.perform(get("/api/auth/captcha"))
                    .andReturn();

            JsonNode j1 = objectMapper.readTree(r1.getResponse().getContentAsString());
            JsonNode j2 = objectMapper.readTree(r2.getResponse().getContentAsString());

            assertThat(j1.at("/data/captchaId").asText())
                    .isNotEqualTo(j2.at("/data/captchaId").asText());
        }
    }

    // ========================================================================
    // 12.5 Configurable Character Set
    //
    // User Story: As an administrator, I can configure the CAPTCHA character set
    //             to include letters (e.g. alphanumeric) for stronger security.
    //
    // Acceptance Criteria:
    //   - Default chars is "0123456789" (backward compatible)
    //   - Custom chars are used for code generation
    //   - Verification is case-insensitive for alphanumeric codes
    // ========================================================================

    @Nested
    @DisplayName("12.5 Configurable Character Set")
    class ConfigurableCharSet {

        @Test
        @DisplayName("default chars generates numeric-only code")
        void defaultChars_numericOnly() {
            // 預設字元集是 0-9，產生的 code 應該全是數字
            for (int i = 0; i < 10; i++) {
                CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
                String answer = getCaptchaAnswer(result.captchaId());
                assertThat(answer).matches("^[0-9]+$");
            }
        }

        @Test
        @DisplayName("verification is case-insensitive")
        void verify_caseInsensitive() {
            // 直接用 alphanumeric 的 CaptchaService 測試 case-insensitive
            CareSecurityProperties.Captcha alphaConfig = new CareSecurityProperties.Captcha();
            alphaConfig.setChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
            alphaConfig.setLength(4);
            alphaConfig.setExpireSeconds(300);

            @SuppressWarnings("unchecked")
            RedisTemplate<String, Object> redis = (RedisTemplate<String, Object>)
                    org.springframework.test.util.ReflectionTestUtils.getField(captchaService, "redisTemplate");

            CaptchaService alphaService = new CaptchaService(redis, alphaConfig);
            CaptchaService.CaptchaResult result = alphaService.generateCaptcha();
            String answer = getCaptchaAnswer(result.captchaId());

            // 用全小寫驗證應該也要通過
            assertThat(alphaService.verifyCaptcha(result.captchaId(), answer.toLowerCase())).isTrue();
        }
    }

    // ========================================================================
    // 12.6 Configurable Image Size
    //
    // User Story: As an administrator, I can configure the CAPTCHA image dimensions.
    //
    // Acceptance Criteria:
    //   - Default width=160, height=50, fontSize=32
    //   - Custom dimensions produce valid images
    // ========================================================================

    @Nested
    @DisplayName("12.6 Configurable Image Size")
    class ConfigurableImageSize {

        @Test
        @DisplayName("custom image size produces valid PNG with correct dimensions")
        void customSize_producesCorrectDimensions() throws Exception {
            CareSecurityProperties.Captcha customConfig = new CareSecurityProperties.Captcha();
            customConfig.setWidth(200);
            customConfig.setHeight(80);
            customConfig.setFontSize(40);
            customConfig.setLength(4);
            customConfig.setExpireSeconds(300);

            @SuppressWarnings("unchecked")
            RedisTemplate<String, Object> redis = (RedisTemplate<String, Object>)
                    org.springframework.test.util.ReflectionTestUtils.getField(captchaService, "redisTemplate");

            CaptchaService customService = new CaptchaService(redis, customConfig);
            CaptchaService.CaptchaResult result = customService.generateCaptcha();

            byte[] imageBytes = java.util.Base64.getDecoder().decode(result.imageBase64());
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(imageBytes));

            assertThat(img.getWidth()).isEqualTo(200);
            assertThat(img.getHeight()).isEqualTo(80);
        }
    }

    // ========================================================================
    // 12.7 Audio CAPTCHA (Accessibility)
    //
    // User Story: As a visually impaired user, I can listen to the CAPTCHA code.
    //
    // Acceptance Criteria:
    //   - audioEnabled=false -> GET /api/auth/captcha/audio/{id} returns 404
    //   - audioEnabled=true  -> returns valid WAV base64
    //   - Non-existent captchaId -> returns 404
    //   - Audio does not consume the CAPTCHA (can still verify after)
    // ========================================================================

    @Nested
    @DisplayName("12.7 Audio CAPTCHA (Accessibility)")
    class AudioCaptcha {

        @Test
        @DisplayName("audio endpoint returns 404 with error message when audioEnabled is false")
        void audioDisabled_returns404() throws Exception {
            CaptchaService.CaptchaResult result = captchaService.generateCaptcha();

            mockMvc.perform(get("/api/auth/captcha/audio/" + result.captchaId()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Audio CAPTCHA is not enabled"));
        }

        @Test
        @DisplayName("generateAudioBase64 returns valid WAV base64 when enabled")
        void audioEnabled_generatesWav() {
            CareSecurityProperties.Captcha audioConfig = new CareSecurityProperties.Captcha();
            audioConfig.setAudioEnabled(true);
            audioConfig.setLength(4);
            audioConfig.setExpireSeconds(300);

            @SuppressWarnings("unchecked")
            RedisTemplate<String, Object> redis = (RedisTemplate<String, Object>)
                    org.springframework.test.util.ReflectionTestUtils.getField(captchaService, "redisTemplate");

            CaptchaService audioService = new CaptchaService(redis, audioConfig);
            CaptchaService.CaptchaResult result = audioService.generateCaptcha();

            String audioBase64 = audioService.generateAudioBase64(result.captchaId());
            assertThat(audioBase64).isNotBlank();

            // 驗證是合法的 WAV：RIFF header
            byte[] wavBytes = java.util.Base64.getDecoder().decode(audioBase64);
            assertThat(wavBytes.length).isGreaterThan(44); // WAV header >= 44 bytes
            // "RIFF" magic bytes
            assertThat(new String(wavBytes, 0, 4)).isEqualTo("RIFF");
            // "WAVE" format
            assertThat(new String(wavBytes, 8, 4)).isEqualTo("WAVE");
        }

        @Test
        @DisplayName("generateAudioBase64 returns null for non-existent captchaId")
        void audioNonExistentId_returnsNull() {
            CareSecurityProperties.Captcha audioConfig = new CareSecurityProperties.Captcha();
            audioConfig.setAudioEnabled(true);

            @SuppressWarnings("unchecked")
            RedisTemplate<String, Object> redis = (RedisTemplate<String, Object>)
                    org.springframework.test.util.ReflectionTestUtils.getField(captchaService, "redisTemplate");

            CaptchaService audioService = new CaptchaService(redis, audioConfig);
            String audioBase64 = audioService.generateAudioBase64("non-existent-id");

            assertThat(audioBase64).isNull();
        }

        @Test
        @DisplayName("audio does not consume the CAPTCHA — can still verify after")
        void audio_doesNotConsumeCaptcha() {
            CareSecurityProperties.Captcha audioConfig = new CareSecurityProperties.Captcha();
            audioConfig.setAudioEnabled(true);
            audioConfig.setLength(4);
            audioConfig.setExpireSeconds(300);

            @SuppressWarnings("unchecked")
            RedisTemplate<String, Object> redis = (RedisTemplate<String, Object>)
                    org.springframework.test.util.ReflectionTestUtils.getField(captchaService, "redisTemplate");

            CaptchaService audioService = new CaptchaService(redis, audioConfig);
            CaptchaService.CaptchaResult result = audioService.generateCaptcha();
            String answer = getCaptchaAnswer(result.captchaId());

            // 呼叫 audio 後
            String audioBase64 = audioService.generateAudioBase64(result.captchaId());
            assertThat(audioBase64).isNotBlank();

            // CAPTCHA 仍然可以驗證
            assertThat(audioService.verifyCaptcha(result.captchaId(), answer)).isTrue();
        }
    }
}
