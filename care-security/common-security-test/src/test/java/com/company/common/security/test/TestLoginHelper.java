package com.company.common.security.test;

import com.company.common.security.captcha.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;

/**
 * Shared helper for tests that need to create LoginRequest with valid CAPTCHA.
 */
public final class TestLoginHelper {

    private TestLoginHelper() {}

    /**
     * Create a LoginRequest with a valid CAPTCHA (when CaptchaService is available).
     */
    public static LoginRequest loginRequest(String username, String password, CaptchaService captchaService) {
        if (captchaService == null) {
            return new LoginRequest(username, password, null, null);
        }
        CaptchaService.CaptchaResult captcha = captchaService.generateCaptcha();
        String answer = captchaService.getAnswerForTest(captcha.captchaId());
        return new LoginRequest(username, password, captcha.captchaId(), answer);
    }
}
