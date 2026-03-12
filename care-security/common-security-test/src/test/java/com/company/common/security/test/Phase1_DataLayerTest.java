package com.company.common.security.test;

import com.company.common.security.entity.*;
import com.company.common.security.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@CareSecurityTest
@DisplayName("Phase 1: Data Layer - DB Connection + Entity Mapping")
class Phase1_DataLayerTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SaUserRepository saUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermRepository permRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private PwdHistoryRepository pwdHistoryRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrganizeRepository organizeRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("1.1 Database connection - SELECT 1 succeeds")
    void testDatabaseConnection() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("1.2 SaUser table has records")
    void testSaUserTableCount() {
        long count = saUserRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("1.3 Role table has records")
    void testRoleTableCount() {
        long count = roleRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("1.4 Perm table has records")
    void testPermTableCount() {
        long count = permRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("1.5 Find ADMIN user - OBJID=1, ENABLED=true")
    void testFindAdminUser() {
        Optional<SaUser> admin = saUserRepository.findByUsername("ADMIN");
        assertThat(admin).isPresent();
        assertThat(admin.get().getObjid()).isEqualTo(1L);
        assertThat(admin.get().getEnabled()).isTrue();
    }

    @Test
    @DisplayName("1.6 Find ADMIN with roles - has ROLE_ADMIN")
    void testFindAdminUserWithRoles() {
        Optional<SaUser> admin = saUserRepository.findByUsernameWithRoles("ADMIN");
        assertThat(admin).isPresent();
        SaUser user = admin.get();

        Set<String> roleNames = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getAuthority())
                .collect(Collectors.toSet());

        assertThat(roleNames).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("1.7 Find ROLE_ADMIN - OBJID=1")
    void testFindRoleAdmin() {
        Optional<Role> role = roleRepository.findById(1L);
        assertThat(role).isPresent();
        assertThat(role.get().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("1.8 Find ROLE_ADMIN permissions - not empty")
    void testFindPermsByRoleAdmin() {
        List<Perm> perms = permRepository.findByRoleIdWithMenu(1L);
        assertThat(perms).isNotEmpty();
    }

    @Test
    @DisplayName("1.9 LoginHistory table is accessible")
    void testLoginHistoryAccessible() {
        long count = loginHistoryRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("1.10 PwdHistory table is accessible")
    void testPwdHistoryAccessible() {
        long count = pwdHistoryRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("1.11 Menu table is not empty")
    void testMenuTableNotEmpty() {
        long count = menuRepository.count();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("1.12 Organize table is not empty")
    void testOrganizeTableNotEmpty() {
        long count = organizeRepository.count();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("1.13 Redis connection - PING returns PONG")
    void testRedisConnection() {
        String result = redisTemplate.getConnectionFactory().getConnection().ping();
        assertThat(result).isEqualTo("PONG");
    }
}
