package com.company.common.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OTP setup response with secret and QR code URI")
public record OtpSetupResponse(
        @Schema(description = "Base32-encoded TOTP secret") String secret,
        @Schema(description = "otpauth:// URI for QR code generation") String otpAuthUri
) {}
