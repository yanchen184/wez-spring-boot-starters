package com.company.common.security.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.util.List;

/**
 * Adds minimal identity claims to JWT access tokens.
 * <p>
 * JWT claims (slim):
 * {
 *   "roles": ["ROLE_ADMIN"],
 *   "userId": 1,
 *   "cname": "管理員",
 *   "authSource": "LOCAL"
 * }
 * <p>
 * Permissions and org roles are NOT in JWT — use GET /api/auth/me instead.
 */
public class JwtTokenCustomizer {

    private final CustomUserDetailsService userDetailsService;

    public JwtTokenCustomizer(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public void customize(JwtEncodingContext context) {
        Authentication authentication = context.getPrincipal();
        if (authentication == null) return;

        String username = authentication.getName();

        CustomUserDetails userDetails;
        try {
            userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            return;
        }

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        context.getClaims().claim("roles", roles);
        context.getClaims().claim("userId", userDetails.getUserId());
        context.getClaims().claim("cname", userDetails.getCname());
        context.getClaims().claim("authSource", userDetails.getAuthSource());
    }
}
