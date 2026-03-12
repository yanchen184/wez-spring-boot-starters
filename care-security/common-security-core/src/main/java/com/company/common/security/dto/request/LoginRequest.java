package com.company.common.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "POST /api/auth/login")
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,
        @NotBlank(message = "Password is required")
        String password,
        @Schema(description = "CAPTCHA ID from GET /api/auth/captcha (required when captcha is enabled)")
        String captchaId,
        @Schema(description = "User's answer to the CAPTCHA image (required when captcha is enabled)")
        String captchaAnswer
) {}
