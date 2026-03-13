package com.company.common.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "POST /api/auth/cert/login — MOICA citizen certificate login request (PKCS#7)")
public record CertLoginRequest(
        @NotBlank(message = "Login token is required")
        @Schema(description = "Login token from GET /api/auth/cert/login-token")
        String loginToken,

        @NotBlank(message = "PKCS#7 signed data is required")
        @Schema(description = "Base64-encoded PKCS#7 (CMS) SignedData containing the certificate, signature, and CardSN")
        String base64Data
) {}
