package com.company.common.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "JWT Token response (login / refresh)")
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        List<String> roles,
        @Schema(description = "Current organization ID (null = global mode)") Long currentOrgId,
        @Schema(description = "Original admin username when impersonating (null = normal login)") String impersonatedBy,
        @Schema(description = "Authentication source: LOCAL or LDAP") String authSource,
        @Schema(description = "True if OTP verification is required before tokens are issued") boolean requiresOtp
) {
    public TokenResponse(String accessToken, String refreshToken, String tokenType,
                         long expiresIn, String username, List<String> roles,
                         Long currentOrgId, String impersonatedBy) {
        this(accessToken, refreshToken, tokenType, expiresIn, username, roles, currentOrgId, impersonatedBy, "LOCAL", false);
    }

    public TokenResponse(String accessToken, String refreshToken, String tokenType,
                         long expiresIn, String username, List<String> roles,
                         Long currentOrgId, String impersonatedBy, String authSource) {
        this(accessToken, refreshToken, tokenType, expiresIn, username, roles, currentOrgId, impersonatedBy, authSource, false);
    }

    public static TokenResponse otpRequired(String username) {
        return new TokenResponse(null, null, null, 0, username, List.of(), null, null, null, true);
    }
}
