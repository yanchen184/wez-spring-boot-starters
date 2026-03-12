package com.company.common.security.dto.response;

import java.util.List;

public record OrgRoleResponse(
        Long orgId,
        String orgName,
        String orgCode,
        List<OrgRoleAssignment> assignments
) {
    public record OrgRoleAssignment(
            Long id,
            Long userId,
            String username,
            String cname,
            Long roleId,
            String roleAuthority
    ) {}
}
