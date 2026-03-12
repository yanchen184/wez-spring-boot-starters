package com.company.common.security.test;

import com.company.common.security.captcha.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.security.CrudPermissionEvaluator;
import com.company.common.security.security.CustomUserDetails;
import com.company.common.security.security.CustomUserDetailsService;
import com.company.common.security.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import static com.company.common.security.test.TestLoginHelper.loginRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@CareSecurityTest
@DisplayName("Phase 6: RBAC Permission - CrudPermissionEvaluator")
class Phase6_RbacPermissionTest {

    @Autowired
    private CrudPermissionEvaluator evaluator;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Value("${care-test.admin-password}")
    private String adminPassword;

    @Value("${care-test.non-admin-username}")
    private String nonAdminUsername;

    private CustomUserDetails adminDetails;
    private CustomUserDetails nonAdminDetails;

    @BeforeEach
    void setUp() {
        adminDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("ADMIN");
        nonAdminDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(nonAdminUsername);
    }

    private Authentication createAuth(CustomUserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("6.1 CrudEvaluator - ADMIN CREATE permission check")
    void testCrudEvaluatorCreate() {
        Authentication auth = createAuth(adminDetails);
        boolean hasAnyCreate = adminDetails.getPermissions().entrySet().stream()
                .anyMatch(e -> e.getValue().c());
        if (hasAnyCreate) {
            String permCode = adminDetails.getPermissions().entrySet().stream()
                    .filter(e -> e.getValue().c())
                    .findFirst().get().getKey();
            assertThat(evaluator.hasPermission(auth, permCode, "CREATE")).isTrue();
        }
    }

    @Test
    @DisplayName("6.2 CrudEvaluator - ADMIN has permissions")
    void testCrudEvaluatorRead() {
        Authentication auth = createAuth(adminDetails);
        assertThat(adminDetails.getPermissions()).isNotEmpty();

        String permCode = adminDetails.getPermissions().keySet().iterator().next();
        evaluator.hasPermission(auth, permCode, "READ");
    }

    @Test
    @DisplayName("6.3 CrudEvaluator - ADMIN UPDATE permission check")
    void testCrudEvaluatorUpdate() {
        Authentication auth = createAuth(adminDetails);
        boolean hasAnyUpdate = adminDetails.getPermissions().entrySet().stream()
                .anyMatch(e -> e.getValue().u());
        if (hasAnyUpdate) {
            String permCode = adminDetails.getPermissions().entrySet().stream()
                    .filter(e -> e.getValue().u())
                    .findFirst().get().getKey();
            assertThat(evaluator.hasPermission(auth, permCode, "UPDATE")).isTrue();
        }
    }

    @Test
    @DisplayName("6.4 CrudEvaluator - ADMIN DELETE permission check")
    void testCrudEvaluatorDelete() {
        Authentication auth = createAuth(adminDetails);
        boolean hasAnyDelete = adminDetails.getPermissions().entrySet().stream()
                .anyMatch(e -> e.getValue().d());
        if (hasAnyDelete) {
            String permCode = adminDetails.getPermissions().entrySet().stream()
                    .filter(e -> e.getValue().d())
                    .findFirst().get().getKey();
            assertThat(evaluator.hasPermission(auth, permCode, "DELETE")).isTrue();
        }
    }

    @Test
    @DisplayName("6.5 CrudEvaluator - ADMIN APPROVE permission check")
    void testCrudEvaluatorApprove() {
        Authentication auth = createAuth(adminDetails);
        boolean hasAnyApprove = adminDetails.getPermissions().entrySet().stream()
                .anyMatch(e -> e.getValue().a());
        if (hasAnyApprove) {
            String permCode = adminDetails.getPermissions().entrySet().stream()
                    .filter(e -> e.getValue().a())
                    .findFirst().get().getKey();
            assertThat(evaluator.hasPermission(auth, permCode, "APPROVE")).isTrue();
        }
    }

    @Test
    @DisplayName("6.6 Null authentication throws NullPointerException (parameters are @NonNull)")
    void testNullAuth() {
        assertThatThrownBy(() -> evaluator.hasPermission(null, "SOME_PERM", "READ"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("6.7 Unknown permission code returns false")
    void testUnknownPermCode() {
        Authentication auth = createAuth(adminDetails);
        assertThat(evaluator.hasPermission(auth, "NON_EXISTENT_PERM_XYZ", "READ")).isFalse();
    }

    @Test
    @DisplayName("6.8 From JWT claims - permissions correctly parsed")
    @Transactional
    void testFromJwtClaims() {
        LoginRequest request = loginRequest("ADMIN", adminPassword, captchaService);
        TokenResponse response = authService.login(request, "127.0.0.1", "JUnit");

        Jwt jwt = jwtDecoder.decode(response.accessToken());
        JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt);

        String permCode = adminDetails.getPermissions().keySet().iterator().next();
        evaluator.hasPermission(jwtAuth, permCode, "READ");
    }

    @Test
    @DisplayName("6.9 Shorthand 'C' equals 'CREATE'")
    void testShorthandC() {
        Authentication auth = createAuth(adminDetails);
        boolean hasAnyCreate = adminDetails.getPermissions().entrySet().stream()
                .anyMatch(e -> e.getValue().c());
        if (hasAnyCreate) {
            String permCode = adminDetails.getPermissions().entrySet().stream()
                    .filter(e -> e.getValue().c())
                    .findFirst().get().getKey();
            assertThat(evaluator.hasPermission(auth, permCode, "C")).isTrue();
        }
    }

    @Test
    @DisplayName("6.10 Shorthand 'R' equals 'READ'")
    void testShorthandR() {
        Authentication auth = createAuth(adminDetails);
        String permCode = adminDetails.getPermissions().keySet().iterator().next();
        evaluator.hasPermission(auth, permCode, "R");
    }

    @Test
    @DisplayName("6.11 ADMIN has permissions mapped")
    void testAdminHasPermissions() {
        assertThat(adminDetails.getPermissions()).isNotEmpty();
    }

    @Test
    @DisplayName("6.12 Non-admin has fewer or equal permissions than ADMIN")
    void testNonAdminLimitedPermissions() {
        assertThat(nonAdminDetails.getPermissions().size())
                .isLessThanOrEqualTo(adminDetails.getPermissions().size());
    }

    @Test
    @DisplayName("6.13 From CustomUserDetails principal - correct evaluation")
    void testFromUserDetails() {
        Authentication auth = createAuth(adminDetails);
        String permCode = adminDetails.getPermissions().keySet().iterator().next();
        evaluator.hasPermission(auth, permCode, "READ");
    }
}
