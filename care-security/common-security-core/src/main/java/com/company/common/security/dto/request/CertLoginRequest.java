package com.company.common.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "POST /api/auth/cert/login — Citizen certificate login request")
public record CertLoginRequest(
        @NotBlank(message = "Challenge ID is required")
        @Schema(description = "Challenge ID from GET /api/auth/cert/challenge")
        String challengeId,

        @NotBlank(message = "Certificate is required")
        @Schema(description = "Base64-encoded X.509 certificate (DER format)")
        String certificate,

        @NotBlank(message = "Signature is required")
        @Schema(description = "Base64-encoded digital signature of the challenge nonce")
        String signature
) {}
