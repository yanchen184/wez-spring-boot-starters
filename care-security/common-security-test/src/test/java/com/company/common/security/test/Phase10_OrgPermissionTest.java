package com.company.common.security.test;

import tools.jackson.databind.ObjectMapper;
import com.company.common.security.service.CaptchaService;
import com.company.common.security.dto.request.LoginRequest;
// import com.company.common.security.dto.request.SwitchOrgRequest; // Removed: switch-org feature disabled
import com.company.common.security.dto.response.MyOrgResponse;
import com.company.common.security.dto.response.TokenResponse;
import com.company.common.security.repository.PermRepository;
import com.company.common.security.repository.SaUserOrgRoleRepository;
import com.company.common.security.security.CustomUserDetails;
import com.company.common.security.security.CustomUserDetailsService;
import com.company.common.security.service.AuthService;
import com.company.common.security.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.company.common.security.test.TestLoginHelper.loginRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@CareSecurityTest
@AutoConfigureMockMvc
@Import({com.company.common.security.controller.AuthController.class,
         com.company.common.security.controller.UserController.class,
         com.company.common.security.exception.SecurityExceptionHandler.class})
@DisplayName("Phase 10: Organization Permission Switching")
class Phase10_OrgPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PermRepository permRepository;

    @Autowired
    private SaUserOrgRoleRepository saUserOrgRoleRepository;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Value("${care-test.admin-password}")
    private String adminPassword;

    @Value("${care-test.non-admin-username}")
    private String nonAdminUsername;

    // ===== Service Layer Tests =====

    @Nested
    @DisplayName("CustomUserDetailsService - Org Filtering")
    class UserDetailsServiceTests {

        @Test
        @DisplayName("10.1 loadUserByUsernameAndOrg(null) returns all roles (backward compatible)")
        @Transactional(readOnly = true)
        void testLoadUserWithNullOrg() {
            CustomUserDetails details = userDetailsService.loadUserByUsernameAndOrg("ADMIN", null);

            assertThat(details).isNotNull();
            assertThat(details.getCurrentOrgId()).isNull();
            assertThat(details.getAuthorities()).isNotEmpty();
            // orgRoles should always be full
            assertThat(details.getOrgRoles()).isNotEmpty();
        }

        @Test
        @DisplayName("10.2 loadUserByUsernameAndOrg(orgId) filters authorities to global + specified org")
        @Transactional(readOnly = true)
        void testLoadUserWithSpecificOrg() {
            // ADMIN has org roles in org=1 (ROLE_EXAMPLE, ROLE_SUPERVISOR, ROLE_TEST)
            CustomUserDetails allDetails = userDetailsService.loadUserByUsernameAndOrg("ADMIN", null);
            CustomUserDetails orgDetails = userDetailsService.loadUserByUsernameAndOrg("ADMIN", 1L);

            assertThat(orgDetails.getCurrentOrgId()).isEqualTo(1L);
            // orgRoles should always be full regardless of filter
            assertThat(orgDetails.getOrgRoles()).hasSameSizeAs(allDetails.getOrgRoles());
        }

        @Test
        @DisplayName("10.3 loadUserByUsernameAndOrg(orgId) with no org roles returns only global roles")
        @Transactional(readOnly = true)
        void testLoadUserWithNonExistentOrg() {
            // Use an org ID that the user doesn't belong to
            CustomUserDetails details = userDetailsService.loadUserByUsernameAndOrg("ADMIN", 99999L);

            assertThat(details.getCurrentOrgId()).isEqualTo(99999L);
            // Should still have global roles
            assertThat(details.getAuthorities()).isNotEmpty();
        }

        @Test
        @DisplayName("10.4 loadUserByUsername delegates to loadUserByUsernameAndOrg(null)")
        @Transactional(readOnly = true)
        void testLoadUserByUsernameDelegatesToOrgMethod() {
            CustomUserDetails viaOldMethod = (CustomUserDetails) userDetailsService.loadUserByUsername("ADMIN");
            CustomUserDetails viaNewMethod = userDetailsService.loadUserByUsernameAndOrg("ADMIN", null);

            assertThat(viaOldMethod.getCurrentOrgId()).isNull();
            assertThat(viaNewMethod.getCurrentOrgId()).isNull();
            assertThat(viaOldMethod.getAuthorities()).hasSameSizeAs(viaNewMethod.getAuthorities());
            assertThat(viaOldMethod.getPermissions()).hasSameSizeAs(viaNewMethod.getPermissions());
        }
    }

    // ===== Repository Layer Tests =====

    @Nested
    @DisplayName("PermRepository - Org-aware Queries")
    class PermRepositoryTests {

        @Test
        @DisplayName("10.5 findByUserIdAllRoles returns perms from both global and org roles")
        @Transactional(readOnly = true)
        void testFindByUserIdAllRoles() {
            // ADMIN (id=1) has global roles (ROLE_ADMIN=1) and org roles
            var globalPerms = permRepository.findByUserIdAllRoles(1L);
            var allPerms = permRepository.findByUserIdAllRoles(1L);

            // allRoles should return >= global only (since it also includes org role perms)
            assertThat(allPerms.size()).isGreaterThanOrEqualTo(globalPerms.size());
        }

        @Test
        @DisplayName("10.6 findByUserIdAndOrgId returns perms from global + specific org roles")
        @Transactional(readOnly = true)
        void testFindByUserIdAndOrgId() {
            // ADMIN has org role in org=1
            var orgPerms = permRepository.findByUserIdAndOrgId(1L, 1L);
            var globalPerms = permRepository.findByUserIdAllRoles(1L);

            // Should have at least the global permissions
            assertThat(orgPerms.size()).isGreaterThanOrEqualTo(globalPerms.size());
        }

        @Test
        @DisplayName("10.7 findByUserIdAndOrgId with non-existent org returns only global perms")
        @Transactional(readOnly = true)
        void testFindByUserIdAndNonExistentOrg() {
            var globalPerms = permRepository.findByUserIdAllRoles(1L);
            var orgPerms = permRepository.findByUserIdAndOrgId(1L, 99999L);

            // With non-existent org, should match global-only
            assertThat(orgPerms).hasSameSizeAs(globalPerms);
        }
    }

    // ===== SaUserOrgRoleRepository Tests =====

    @Nested
    @DisplayName("SaUserOrgRoleRepository")
    class OrgRoleRepositoryTests {

        @Test
        @DisplayName("10.8 findByOrganizeObjid returns users in the organization")
        @Transactional(readOnly = true)
        void testFindByOrganizeObjid() {
            // Org 1 (所有單位) should have users assigned
            var orgRoles = saUserOrgRoleRepository.findByOrganizeObjid(1L);
            assertThat(orgRoles).isNotEmpty();
        }

        @Test
        @DisplayName("10.9 findByOrganizeObjid with non-existent org returns empty")
        @Transactional(readOnly = true)
        void testFindByNonExistentOrg() {
            var orgRoles = saUserOrgRoleRepository.findByOrganizeObjid(99999L);
            assertThat(orgRoles).isEmpty();
        }
    }

    // ===== AuthService Tests =====

    @Nested
    @DisplayName("AuthService - Org Switching")
    class AuthServiceTests {

        @Test
        @DisplayName("10.10 login returns null currentOrgId (global mode)")
        @Transactional
        void testLoginReturnsNullOrgId() {
            TokenResponse response = authService.login(
                    loginRequest("ADMIN", adminPassword, captchaService), "127.0.0.1", "JUnit");

            assertThat(response.currentOrgId()).isNull();
        }

        // Removed: switch-org feature disabled in demo
        /*
        @Test
        @DisplayName("10.11 switchOrg returns token with currentOrgId set")
        @Transactional
        void testSwitchOrgReturnsOrgId() {
            // ADMIN has org role in org=1
            TokenResponse response = authService.switchOrg("ADMIN", 1L);

            assertThat(response.currentOrgId()).isEqualTo(1L);
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
        }

        @Test
        @DisplayName("10.12 switchOrg JWT contains currentOrgId claim")
        @Transactional
        void testSwitchOrgJwtContainsOrgId() {
            TokenResponse response = authService.switchOrg("ADMIN", 1L);

            Jwt jwt = jwtDecoder.decode(response.accessToken());
            assertThat(jwt.hasClaim("currentOrgId")).isTrue();
            assertThat(((Number) jwt.getClaim("currentOrgId")).longValue()).isEqualTo(1L);
        }

        @Test
        @DisplayName("10.13 switchOrg to non-belonging org throws exception")
        @Transactional
        void testSwitchOrgToNonBelongingOrg() {
            // EDWARDLIN (id=30127) only has org role in org=30102
            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.security.authentication.BadCredentialsException.class,
                    () -> authService.switchOrg("EDWARDLIN", 99999L));
        }
        */

        @Test
        @DisplayName("10.14 getMyOrgs returns user's organizations with roles")
        @Transactional(readOnly = true)
        void testGetMyOrgs() {
            List<MyOrgResponse> orgs = authService.getMyOrgs("ADMIN");

            assertThat(orgs).isNotEmpty();
            // Each org should have roles
            for (MyOrgResponse org : orgs) {
                assertThat(org.orgId()).isNotNull();
                assertThat(org.roles()).isNotEmpty();
            }
        }

        // Removed: switch-org feature disabled in demo
        /*
        @Test
        @DisplayName("10.15 refresh preserves currentOrgId from refresh token")
        @Transactional
        void testRefreshPreservesOrgId() {
            // First switch to org
            TokenResponse switchResponse = authService.switchOrg("ADMIN", 1L);

            // Then refresh
            TokenResponse refreshResponse = authService.refresh(switchResponse.refreshToken());

            assertThat(refreshResponse.currentOrgId()).isEqualTo(1L);
        }
        */

        @Test
        @DisplayName("10.16 JWT orgRoles always contains orgName field")
        @Transactional
        void testJwtOrgRolesContainOrgName() {
            TokenResponse response = authService.login(
                    loginRequest("ADMIN", adminPassword, captchaService), "127.0.0.1", "JUnit");

            Jwt jwt = jwtDecoder.decode(response.accessToken());
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> orgRoles = jwt.getClaim("orgRoles");
            if (orgRoles != null && !orgRoles.isEmpty()) {
                assertThat(orgRoles.getFirst()).containsKey("orgName");
                assertThat(orgRoles.getFirst()).containsKey("orgId");
                assertThat(orgRoles.getFirst()).containsKey("roleAuthority");
            }
        }
    }

    // ===== Controller Integration Tests =====

    @Nested
    @DisplayName("AuthController - Switch Org & My Orgs Endpoints")
    class AuthControllerTests {

        private String adminAccessToken;

        @BeforeEach
        void setUp() {
            TokenResponse response = authService.login(
                    loginRequest("ADMIN", adminPassword, captchaService), "127.0.0.1", "JUnit");
            adminAccessToken = response.accessToken();
        }

        // Removed: switch-org feature disabled in demo
        /*
        @Test
        @DisplayName("10.17 POST /api/auth/switch-org returns 200 with new token")
        @Transactional
        void testSwitchOrgEndpoint() throws Exception {
            SwitchOrgRequest request = new SwitchOrgRequest(1L);

            mockMvc.perform(post("/api/auth/switch-org")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.currentOrgId").value(1));
        }

        @Test
        @DisplayName("10.18 POST /api/auth/switch-org without auth returns 401")
        void testSwitchOrgWithoutAuth() throws Exception {
            SwitchOrgRequest request = new SwitchOrgRequest(1L);

            mockMvc.perform(post("/api/auth/switch-org")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
        */

        @Test
        @DisplayName("10.19 GET /api/auth/my-orgs returns user's organizations")
        @Transactional
        void testMyOrgsEndpoint() throws Exception {
            mockMvc.perform(get("/api/auth/my-orgs")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ===== UserController Tests =====

    @Nested
    @DisplayName("UserController - Org Filtering")
    class UserControllerTests {

        private String adminAccessToken;

        @BeforeEach
        void setUp() {
            TokenResponse response = authService.login(
                    loginRequest("ADMIN", adminPassword, captchaService), "127.0.0.1", "JUnit");
            adminAccessToken = response.accessToken();
        }

        @Test
        @DisplayName("10.20 GET /api/users returns paged result with content array")
        @Transactional
        void testFindAllWithoutOrgFilter() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.totalElements").isNumber());
        }

        @Test
        @DisplayName("10.21 GET /api/users?orgId=1 returns paged users in that org")
        @Transactional
        void testFindAllWithOrgFilter() throws Exception {
            mockMvc.perform(get("/api/users")
                            .param("orgId", "1")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("10.22 GET /api/users?orgId=99999 returns empty paged result")
        @Transactional
        void testFindAllWithNonExistentOrg() throws Exception {
            mockMvc.perform(get("/api/users")
                            .param("orgId", "99999")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    // ===== Backward Compatibility Tests =====

    @Nested
    @DisplayName("Backward Compatibility")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("10.23 TokenResponse without currentOrgId uses default constructor")
        void testTokenResponseBackwardCompat() {
            TokenResponse response = new TokenResponse("access", "refresh", "Bearer", 1800, "user", List.of("ROLE_USER"), null, null);
            assertThat(response.currentOrgId()).isNull();
        }

        @Test
        @DisplayName("10.24 CustomUserDetails 12-param constructor sets currentOrgId=null")
        void testCustomUserDetailsBackwardCompat() {
            CustomUserDetails details = new CustomUserDetails(
                    1L, "test", "pass", null, "Test", true, false, false, false,
                    List.of(), List.of(), java.util.Map.of());
            assertThat(details.getCurrentOrgId()).isNull();
        }

        @Test
        @DisplayName("10.25 CustomUserDetails 13-param constructor sets currentOrgId")
        void testCustomUserDetailsWithOrgId() {
            CustomUserDetails details = new CustomUserDetails(
                    1L, "test", "pass", null, "Test", true, false, false, false,
                    List.of(), List.of(), java.util.Map.of(), 5L);
            assertThat(details.getCurrentOrgId()).isEqualTo(5L);
        }
    }
}
