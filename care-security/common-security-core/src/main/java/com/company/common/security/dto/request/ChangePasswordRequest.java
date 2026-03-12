package com.company.common.security.dto.request;

import com.company.common.security.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "POST /api/auth/change-password")
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @ValidPassword
        String newPassword
) {}
