package com.company.common.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "POST /api/users/{id}/org-roles")
public record AssignOrgRoleRequest(
        @Schema(description = "Organization ID, null for global role")
        Long orgId,
        @NotNull(message = "Role ID is required")
        Long roleId
) {}
