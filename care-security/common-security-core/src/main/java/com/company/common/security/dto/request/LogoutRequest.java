package com.company.common.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "POST /api/auth/logout")
public record LogoutRequest(
        String refreshToken
) {}
