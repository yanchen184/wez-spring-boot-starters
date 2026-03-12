package com.company.common.security.dto.request;

public record PermRequest(
        String permCode,
        Long menuId,
        Boolean canCreate,
        Boolean canRead,
        Boolean canUpdate,
        Boolean canDelete,
        Boolean canApprove
) {}
