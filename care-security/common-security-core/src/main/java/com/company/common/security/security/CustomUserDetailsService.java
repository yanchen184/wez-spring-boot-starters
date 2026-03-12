package com.company.common.security.security;

import com.company.common.security.entity.Perm;
import com.company.common.security.entity.SaUser;
import com.company.common.security.repository.PermRepository;
import com.company.common.security.repository.SaUserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomUserDetailsService implements UserDetailsService {

    private final SaUserRepository saUserRepository;
    private final PermRepository permRepository;

    public CustomUserDetailsService(SaUserRepository saUserRepository,
                                    PermRepository permRepository) {
        this.saUserRepository = saUserRepository;
        this.permRepository = permRepository;
    }

    @NonNull
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return loadUserByUsernameAndOrg(username, null);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadUserByUsernameAndOrg(String username, Long orgId) throws UsernameNotFoundException {
        SaUser user = saUserRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Collect authorities: global roles (organize == null) always included
        Set<GrantedAuthority> authorities = user.getUserRoles().stream()
                .filter(ur -> ur.getOrganize() == null)
                .map(ur -> new SimpleGrantedAuthority(ur.getRole().getAuthority()))
                .collect(Collectors.toSet());

        if (orgId != null) {
            // Also add roles from the specified organization
            user.getUserRoles().stream()
                    .filter(ur -> ur.getOrganize() != null && orgId.equals(ur.getOrganize().getObjid()))
                    .map(ur -> new SimpleGrantedAuthority(ur.getRole().getAuthority()))
                    .forEach(authorities::add);
        } else {
            // Add all org-level roles (backward compatible)
            user.getUserRoles().stream()
                    .filter(ur -> ur.getOrganize() != null)
                    .map(ur -> new SimpleGrantedAuthority(ur.getRole().getAuthority()))
                    .forEach(authorities::add);
        }

        // orgRoles: only org-scoped entries (frontend needs it for org switcher)
        List<CustomUserDetails.OrgRole> orgRoles = user.getUserRoles().stream()
                .filter(ur -> ur.getOrganize() != null)
                .map(ur -> {
                    Long oId = ur.getOrganize().getObjid();
                    String orgName = ur.getOrganize().getOrgName();
                    return new CustomUserDetails.OrgRole(oId, orgName, ur.getRole().getAuthority());
                })
                .toList();

        // Build CRUD permission map filtered by org
        Map<String, CustomUserDetails.CrudPermission> permissions = buildPermissionMapForOrg(user, orgId);

        return new CustomUserDetails(
                user.getObjid(),
                user.getUsername(),
                user.getPassword(),
                user.getPasswordSalt(),
                user.getCname(),
                Boolean.TRUE.equals(user.getEnabled()),
                Boolean.TRUE.equals(user.getAccountLocked()),
                Boolean.TRUE.equals(user.getAccountExpired()),
                Boolean.TRUE.equals(user.getPasswordExpired()),
                authorities,
                orgRoles,
                permissions,
                orgId,
                user.getAuthSource() != null ? user.getAuthSource() : "LOCAL",
                Boolean.TRUE.equals(user.getOtpEnabled())
        );
    }

    private Map<String, CustomUserDetails.CrudPermission> buildPermissionMapForOrg(SaUser user, Long orgId) {
        List<Perm> perms;
        if (orgId != null) {
            perms = permRepository.findByUserIdAndOrgId(user.getObjid(), orgId);
        } else {
            perms = permRepository.findByUserIdAllRoles(user.getObjid());
        }
        return buildPermissionMapFromPerms(perms);
    }

    private Map<String, CustomUserDetails.CrudPermission> buildPermissionMapFromPerms(List<Perm> perms) {
        Map<String, CustomUserDetails.CrudPermission> permMap = new HashMap<>();

        for (Perm p : perms) {
            // Use permCode if available, otherwise fall back to menu code
            String code = p.getPermCode();
            if (code == null && p.getMenu() != null) {
                code = p.getMenu().getMenuCode();
            }
            if (code == null) continue;
            // Merge permissions: if multiple roles grant same perm, OR the flags
            permMap.merge(code,
                    new CustomUserDetails.CrudPermission(
                            Boolean.TRUE.equals(p.getCanCreate()),
                            Boolean.TRUE.equals(p.getCanRead()),
                            Boolean.TRUE.equals(p.getCanUpdate()),
                            Boolean.TRUE.equals(p.getCanDelete()),
                            Boolean.TRUE.equals(p.getCanApprove())),
                    (existing, incoming) -> new CustomUserDetails.CrudPermission(
                            existing.c() || incoming.c(),
                            existing.r() || incoming.r(),
                            existing.u() || incoming.u(),
                            existing.d() || incoming.d(),
                            existing.a() || incoming.a()));
        }
        return permMap;
    }
}
