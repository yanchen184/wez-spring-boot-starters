package com.company.common.security.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(
        @NotBlank String code
) {}
