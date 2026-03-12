package com.company.common.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "User's organization with roles")
public record MyOrgResponse(
        @Schema(description = "Organization ID") Long orgId,
        @Schema(description = "Organization name") String orgName,
        @Schema(description = "Roles in this organization") List<String> roles
) {}
