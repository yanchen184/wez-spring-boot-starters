package com.company.common.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Switch user (impersonation) request")
public record SwitchUserRequest(
        @NotBlank @Schema(description = "Target username to impersonate") String username,
        @Schema(description = "Optional organization ID to switch into") Long orgId
) {}
