package com.company.common.security.controller;

import com.company.common.response.dto.ApiResponse;
import com.company.common.security.security.LdapAuthenticationProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "LDAP", description = "LDAP configuration and status")
@RestController
@RequestMapping("/api/ldap")
public class LdapController {

    private final LdapAuthenticationProvider ldapAuthProvider;

    public LdapController(org.springframework.beans.factory.ObjectProvider<LdapAuthenticationProvider> ldapAuthProvider) {
        this.ldapAuthProvider = ldapAuthProvider.getIfAvailable();
    }

    @Operation(summary = "LDAP status", description = "Check if LDAP authentication is enabled and connection status")
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        boolean enabled = ldapAuthProvider != null;
        boolean connected = enabled && ldapAuthProvider.testConnection();

        Map<String, Object> status = Map.of(
                "enabled", enabled,
                "connected", connected
        );
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
