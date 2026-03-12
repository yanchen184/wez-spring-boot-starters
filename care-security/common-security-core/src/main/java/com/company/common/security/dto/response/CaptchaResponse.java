package com.company.common.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "CAPTCHA generation response")
public record CaptchaResponse(
        @Schema(description = "Unique CAPTCHA ID, pass back in login request")
        String captchaId,
        @Schema(description = "Base64-encoded PNG image of the verification code")
        String image
) {}
