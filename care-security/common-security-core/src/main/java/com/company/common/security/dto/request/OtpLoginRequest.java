package com.company.common.security.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OtpLoginRequest(
        @NotBlank String username,
        @NotBlank String code
) {}
