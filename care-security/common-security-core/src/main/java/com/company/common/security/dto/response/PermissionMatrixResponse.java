package com.company.common.security.dto.response;

import java.util.List;

public record PermissionMatrixResponse(
        Long roleId,
        String roleAuthority,
        List<PermEntry> permissions
) {
    public record PermEntry(
            Long permId,
            String permCode,
            String permName,
            boolean canCreate,
            boolean canRead,
            boolean canUpdate,
            boolean canDelete,
            boolean canApprove
    ) {}
}
