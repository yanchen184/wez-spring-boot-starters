package com.company.common.security.dto.response;

public record RoleResponse(
        Long id,
        String authority,
        String description,
        Boolean enabled
) {}
