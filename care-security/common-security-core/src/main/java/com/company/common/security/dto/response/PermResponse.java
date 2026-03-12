package com.company.common.security.dto.response;

public record PermResponse(
        Long id,
        String permCode,
        Long menuId,
        String menuName,
        String menuCode,
        String menuUrl,
        Boolean canCreate,
        Boolean canRead,
        Boolean canUpdate,
        Boolean canDelete,
        Boolean canApprove,
        Boolean enabled
) {}
