package com.company.common.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login token for MOICA citizen certificate authentication")
public record LoginTokenResponse(
        @Schema(description = "Login token — the client must sign this value with their citizen certificate via PKCS#7")
        String loginToken
) {}
