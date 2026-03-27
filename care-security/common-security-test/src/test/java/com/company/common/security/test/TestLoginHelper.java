package com.company.common.security.test;

import com.company.common.security.service.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Shared helper for tests that need to create LoginRequest with valid CAPTCHA.
 */
public final class TestLoginHelper {

    private TestLoginHelper() {}

    /**
     * Create a LoginRequest with a valid CAPTCHA (when CaptchaService is available).
     * Reads the answer directly from Redis via reflection on the CaptchaService's redisTemplate.
     */
    @SuppressWarnings("unchecked")
    public static LoginRequest loginRequest(String username, String password, CaptchaService captchaService) {
        if (captchaService == null) {
            return new LoginRequest(username, password, null, null);
        }
        CaptchaService.CaptchaResult captcha = captchaService.generateCaptcha();
        RedisTemplate<String, Object> redis = (RedisTemplate<String, Object>)
                org.springframework.test.util.ReflectionTestUtils.getField(captchaService, "redisTemplate");
        Object stored = redis.opsForValue().get("captcha:" + captcha.captchaId());
        String answer = stored != null ? stored.toString() : null;
        return new LoginRequest(username, password, captcha.captchaId(), answer);
    }
}
