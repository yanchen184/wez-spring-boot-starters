package com.company.common.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "User info response")
public record UserResponse(
        Long id,
        String username,
        String cname,
        String email,
        Boolean enabled,
        Boolean accountLocked,
        LocalDateTime lastLoginTime,
        List<String> roles,
        List<OrgRoleInfo> orgRoles,
        @Schema(description = "Authentication source: LOCAL or LDAP") String authSource
) {
    public record OrgRoleInfo(Long id, Long orgId, String orgName, Long roleId, String roleAuthority) {}
}
