package com.company.common.security.test;

import com.company.common.security.security.CustomUserDetails;
import com.company.common.security.security.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@CareSecurityTest
@DisplayName("Phase 2: UserDetailsService - Load Real Accounts")
class Phase2_UserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Value("${care-test.non-admin-username}")
    private String nonAdminUsername;

    @Test
    @DisplayName("2.1 Load ADMIN user returns CustomUserDetails")
    void testLoadAdminUser() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("ADMIN");
        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        assertThat(userDetails.getUsername()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("2.2 ADMIN has ROLE_ADMIN authority")
    void testAdminHasRoleAdmin() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("ADMIN");
        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("2.3 ADMIN has at least 1 role")
    void testAdminHasRoles() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("ADMIN");
        assertThat(userDetails.getAuthorities()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("2.4 ADMIN has permissions map (non-empty)")
    void testAdminHasPermissions() {
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("ADMIN");
        assertThat(userDetails.getPermissions()).isNotEmpty();
    }

    @Test
    @DisplayName("2.5 ADMIN isEnabled = true")
    void testAdminIsEnabled() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("ADMIN");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("2.6 ADMIN isAccountNonLocked = true")
    void testAdminIsNotLocked() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("ADMIN");
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("2.7 Non-existent user throws UsernameNotFoundException")
    void testLoadNonExistentUser() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("NON_EXISTENT_USER_12345"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("2.8 Non-admin user loads successfully")
    void testLoadNonAdminUser() {
        UserDetails userDetails = userDetailsService.loadUserByUsername(nonAdminUsername);
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(nonAdminUsername);
    }

    @Test
    @DisplayName("2.9 Non-admin user has at least 1 role")
    void testNonAdminHasRoles() {
        UserDetails userDetails = userDetailsService.loadUserByUsername(nonAdminUsername);
        assertThat(userDetails.getAuthorities()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("2.10 Non-admin user does not have ROLE_ADMIN")
    void testNonAdminNoRoleAdmin() {
        UserDetails userDetails = userDetailsService.loadUserByUsername(nonAdminUsername);
        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).doesNotContain("ROLE_ADMIN");
    }

    @Test
    @DisplayName("2.11 Permission merging - ADMIN permissions are not empty")
    void testPermissionMerging() {
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("ADMIN");
        assertThat(userDetails.getPermissions().size()).isGreaterThan(0);
    }
}
