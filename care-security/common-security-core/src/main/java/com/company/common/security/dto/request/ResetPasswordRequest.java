package com.company.common.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "POST /api/users/{id}/reset-password")
public record ResetPasswordRequest(
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128)
        String newPassword
) {}
