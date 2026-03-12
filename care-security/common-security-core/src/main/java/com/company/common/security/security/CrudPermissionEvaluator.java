package com.company.common.security.security;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import java.io.Serializable;
import java.util.Map;

/**
 * Evaluates CRUD permissions by loading from DB via CustomUserDetailsService.
 * <p>
 * JWT no longer carries permissions — this evaluator queries the DB on each check.
 * <p>
 * Usage: @PreAuthorize("hasPermission('CASE_MGMT', 'CREATE')")
 * <p>
 * Permission types: CREATE, READ, UPDATE, DELETE, APPROVE
 */
public class CrudPermissionEvaluator implements PermissionEvaluator {

    private final CustomUserDetailsService userDetailsService;

    public CrudPermissionEvaluator(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean hasPermission(@NonNull Authentication authentication,
                                 @NonNull Object targetDomainObject,
                                 @NonNull Object permission) {
        String permCode = targetDomainObject.toString();
        String action = permission.toString().toUpperCase();
        return checkPermission(authentication, permCode, action);
    }

    @Override
    public boolean hasPermission(@NonNull Authentication authentication,
                                 @Nullable Serializable targetId,
                                 @NonNull String targetType,
                                 @NonNull Object permission) {
        return checkPermission(authentication, targetType, permission.toString().toUpperCase());
    }

    private boolean checkPermission(Authentication authentication, String permCode, String action) {
        Object principal = authentication.getPrincipal();

        Map<String, CustomUserDetails.CrudPermission> permissions;

        if (principal instanceof Jwt jwt) {
            // JWT context: load permissions from DB using username and currentOrgId from token
            String username = jwt.getSubject();
            Long currentOrgId = jwt.hasClaim("currentOrgId") ? jwt.getClaim("currentOrgId") : null;
            CustomUserDetails userDetails = userDetailsService.loadUserByUsernameAndOrg(username, currentOrgId);
            permissions = userDetails.getPermissions();
        } else if (principal instanceof CustomUserDetails userDetails) {
            permissions = userDetails.getPermissions();
        } else {
            return false;
        }

        if (permissions == null || !permissions.containsKey(permCode)) {
            return false;
        }

        CustomUserDetails.CrudPermission perm = permissions.get(permCode);
        return switch (action) {
            case "CREATE", "C" -> perm.c();
            case "READ", "R" -> perm.r();
            case "UPDATE", "U" -> perm.u();
            case "DELETE", "D" -> perm.d();
            case "APPROVE", "A" -> perm.a();
            default -> false;
        };
    }
}
