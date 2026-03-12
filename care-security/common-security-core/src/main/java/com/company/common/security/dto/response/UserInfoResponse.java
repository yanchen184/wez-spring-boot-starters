package com.company.common.security.dto.response;

import com.company.common.security.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Current user info including permissions (returned by GET /api/auth/me)")
public record UserInfoResponse(
        Long userId,
        String username,
        String cname,
        String authSource,
        List<String> roles,
        List<OrgRoleItem> orgRoles,
        Map<String, Map<String, Boolean>> permissions,
        Long currentOrgId,
        boolean otpEnabled
) {

    public record OrgRoleItem(Long orgId, String orgName, String roleAuthority) {}

    public static UserInfoResponse from(CustomUserDetails details) {
        List<String> roles = details.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        List<OrgRoleItem> orgRoles = details.getOrgRoles().stream()
                .map(or -> new OrgRoleItem(or.orgId(), or.orgName(), or.roleAuthority()))
                .toList();

        Map<String, Map<String, Boolean>> permissions =
                CustomUserDetails.toPermissionClaimMap(details.getPermissions());

        return new UserInfoResponse(
                details.getUserId(),
                details.getUsername(),
                details.getCname(),
                details.getAuthSource(),
                roles,
                orgRoles,
                permissions,
                details.getCurrentOrgId(),
                details.isOtpEnabled()
        );
    }
}
