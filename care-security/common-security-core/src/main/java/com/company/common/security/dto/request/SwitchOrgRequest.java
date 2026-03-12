package com.company.common.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Switch organization request")
public record SwitchOrgRequest(
        @NotNull @Schema(description = "Target organization ID") Long orgId
) {}
