package com.company.common.security.security;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String passwordSalt;
    private final String cname;
    private final boolean enabled;
    private final boolean accountLocked;
    private final boolean accountExpired;
    private final boolean passwordExpired;
    private final Collection<? extends GrantedAuthority> authorities;
    private final List<OrgRole> orgRoles;
    private final Map<String, CrudPermission> permissions;
    private final Long currentOrgId;
    private final String authSource;
    private final boolean otpEnabled;

    public CustomUserDetails(Long userId, String username, String password, String passwordSalt,
                             String cname, boolean enabled, boolean accountLocked,
                             boolean accountExpired, boolean passwordExpired,
                             Collection<? extends GrantedAuthority> authorities,
                             List<OrgRole> orgRoles,
                             Map<String, CrudPermission> permissions) {
        this(userId, username, password, passwordSalt, cname, enabled, accountLocked,
             accountExpired, passwordExpired, authorities, orgRoles, permissions, null, "LOCAL", false);
    }

    public CustomUserDetails(Long userId, String username, String password, String passwordSalt,
                             String cname, boolean enabled, boolean accountLocked,
                             boolean accountExpired, boolean passwordExpired,
                             Collection<? extends GrantedAuthority> authorities,
                             List<OrgRole> orgRoles,
                             Map<String, CrudPermission> permissions,
                             Long currentOrgId) {
        this(userId, username, password, passwordSalt, cname, enabled, accountLocked,
             accountExpired, passwordExpired, authorities, orgRoles, permissions, currentOrgId, "LOCAL", false);
    }

    public CustomUserDetails(Long userId, String username, String password, String passwordSalt,
                             String cname, boolean enabled, boolean accountLocked,
                             boolean accountExpired, boolean passwordExpired,
                             Collection<? extends GrantedAuthority> authorities,
                             List<OrgRole> orgRoles,
                             Map<String, CrudPermission> permissions,
                             Long currentOrgId,
                             String authSource) {
        this(userId, username, password, passwordSalt, cname, enabled, accountLocked,
             accountExpired, passwordExpired, authorities, orgRoles, permissions, currentOrgId, authSource, false);
    }

    public CustomUserDetails(Long userId, String username, String password, String passwordSalt,
                             String cname, boolean enabled, boolean accountLocked,
                             boolean accountExpired, boolean passwordExpired,
                             Collection<? extends GrantedAuthority> authorities,
                             List<OrgRole> orgRoles,
                             Map<String, CrudPermission> permissions,
                             Long currentOrgId,
                             String authSource,
                             boolean otpEnabled) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.passwordSalt = passwordSalt;
        this.cname = cname;
        this.enabled = enabled;
        this.accountLocked = accountLocked;
        this.accountExpired = accountExpired;
        this.passwordExpired = passwordExpired;
        this.authorities = authorities != null ? authorities : Collections.emptySet();
        this.orgRoles = orgRoles != null ? orgRoles : Collections.emptyList();
        this.permissions = permissions != null ? permissions : Collections.emptyMap();
        this.currentOrgId = currentOrgId;
        this.authSource = authSource != null ? authSource : "LOCAL";
        this.otpEnabled = otpEnabled;
    }

    public Long getUserId() { return userId; }
    public String getCname() { return cname; }
    public String getPasswordSalt() { return passwordSalt; }
    public List<OrgRole> getOrgRoles() { return orgRoles; }
    public Map<String, CrudPermission> getPermissions() { return permissions; }
    public Long getCurrentOrgId() { return currentOrgId; }
    public String getAuthSource() { return authSource; }
    public boolean isOtpEnabled() { return otpEnabled; }

    @NonNull
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override
    public String getPassword() { return password; }
    @NonNull
    @Override
    public String getUsername() { return username; }
    @Override
    public boolean isAccountNonExpired() { return !accountExpired; }
    @Override
    public boolean isAccountNonLocked() { return !accountLocked; }
    @Override
    public boolean isCredentialsNonExpired() { return !passwordExpired; }
    @Override
    public boolean isEnabled() { return enabled; }

    public record OrgRole(Long orgId, String orgName, String roleAuthority) {}

    public record CrudPermission(boolean c, boolean r, boolean u, boolean d, boolean a) {

        /**
         * Convert to a Map for JWT claim serialization.
         */
        public Map<String, Boolean> toMap() {
            return Map.of("c", c, "r", r, "u", u, "d", d, "a", a);
        }
    }

    /**
     * Convert permission map to JWT-friendly claim format.
     */
    public static Map<String, Map<String, Boolean>> toPermissionClaimMap(
            Map<String, CrudPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, Boolean>> result = new java.util.HashMap<>();
        permissions.forEach((code, perm) -> result.put(code, perm.toMap()));
        return result;
    }
}
