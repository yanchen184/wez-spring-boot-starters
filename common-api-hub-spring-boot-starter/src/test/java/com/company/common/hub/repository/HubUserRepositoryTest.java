package com.company.common.hub.repository;

import com.company.common.hub.TestJpaConfig;
import com.company.common.hub.entity.HubUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HubUserRepository 單元測試。
 */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("HubUserRepository 測試")
class HubUserRepositoryTest {

    @Autowired
    private HubUserRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("應依 username 查到使用者")
    void shouldFindByUsername_whenExists() {
        em.persist(createHubUser("system-a", true));
        em.flush();

        Optional<HubUser> found = repository.findByUsername("system-a");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("system-a");
    }

    @Test
    @DisplayName("username 不存在時應回傳空")
    void shouldReturnEmpty_whenUsernameNotExists() {
        Optional<HubUser> found = repository.findByUsername("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應依 username 查到啟用的使用者")
    void shouldFindByUsernameAndEnabled_whenEnabled() {
        em.persist(createHubUser("system-a", true));
        em.flush();

        Optional<HubUser> found = repository.findByUsernameAndEnabledTrue("system-a");

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("停用的使用者不應被查到")
    void shouldReturnEmpty_whenUserDisabled() {
        em.persist(createHubUser("system-b", false));
        em.flush();

        Optional<HubUser> found = repository.findByUsernameAndEnabledTrue("system-b");

        assertThat(found).isEmpty();
    }

    private HubUser createHubUser(String username, boolean enabled) {
        HubUser hubUser = new HubUser();
        hubUser.setUsername(username);
        hubUser.setPassword("$2a$10$encodedPassword");
        hubUser.setEnabled(enabled);
        return hubUser;
    }
}
