package com.company.common.hub.repository;

import com.company.common.hub.TestJpaConfig;
import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.entity.HubUserSet;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HubUserSetRepository 單元測試。
 */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("HubUserSetRepository 測試")
class HubUserSetRepositoryTest {

    @Autowired
    private HubUserSetRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("應查到使用者對指定 API 的啟用授權")
    void shouldFindByHubUserAndHubSetAndEnabled_whenExists() {
        HubSet hubSet = createAndPersistHubSet("API-1", "/api/users/**");
        HubUser hubUser = createAndPersistHubUser("system-a");
        createAndPersistUserSet(hubUser, hubSet, true);
        em.flush();

        Optional<HubUserSet> found = repository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet);

        assertThat(found).isPresent();
        assertThat(found.get().getHubSet().getName()).isEqualTo("API-1");
    }

    @Test
    @DisplayName("停用的授權不應被查到")
    void shouldReturnEmpty_whenUserSetDisabled() {
        HubSet hubSet = createAndPersistHubSet("API-1", "/api/users/**");
        HubUser hubUser = createAndPersistHubUser("system-a");
        createAndPersistUserSet(hubUser, hubSet, false);
        em.flush();

        Optional<HubUserSet> found = repository.findByHubUserAndHubSetAndEnabledTrue(hubUser, hubSet);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應查到使用者所有啟用的授權")
    void shouldFindAllEnabledByUser_whenMultipleExist() {
        HubSet hubSet1 = createAndPersistHubSet("API-1", "/api/users/**");
        HubSet hubSet2 = createAndPersistHubSet("API-2", "/api/docs/**");
        HubSet hubSet3 = createAndPersistHubSet("API-3", "/api/reports/**");
        HubUser hubUser = createAndPersistHubUser("system-a");

        createAndPersistUserSet(hubUser, hubSet1, true);
        createAndPersistUserSet(hubUser, hubSet2, true);
        createAndPersistUserSet(hubUser, hubSet3, false);
        em.flush();

        List<HubUserSet> result = repository.findByHubUserAndEnabledTrue(hubUser);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("使用者無任何授權時應回傳空列表")
    void shouldReturnEmptyList_whenNoUserSetExists() {
        createAndPersistHubUser("system-lonely");
        em.flush();

        HubUser hubUser = em.createQuery(
                "SELECT u FROM HubUser u WHERE u.username = :username", HubUser.class)
                .setParameter("username", "system-lonely")
                .getSingleResult();

        List<HubUserSet> result = repository.findByHubUserAndEnabledTrue(hubUser);

        assertThat(result).isEmpty();
    }

    private HubSet createAndPersistHubSet(String name, String uri) {
        HubSet hubSet = new HubSet();
        hubSet.setName(name);
        hubSet.setUri(uri);
        hubSet.setEnabled(true);
        em.persist(hubSet);
        return hubSet;
    }

    private HubUser createAndPersistHubUser(String username) {
        HubUser hubUser = new HubUser();
        hubUser.setUsername(username);
        hubUser.setPassword("$2a$10$encoded");
        hubUser.setEnabled(true);
        em.persist(hubUser);
        return hubUser;
    }

    private void createAndPersistUserSet(HubUser hubUser, HubSet hubSet, boolean enabled) {
        HubUserSet userSet = new HubUserSet();
        userSet.setHubSet(hubSet);
        userSet.setHubUser(hubUser);
        userSet.setVerifyDts(LocalDate.of(2026, 1, 1));
        userSet.setVerifyDte(LocalDate.of(2026, 12, 31));
        userSet.setUserVerify(true);
        userSet.setJwtTokenVerify(true);
        userSet.setEnabled(enabled);
        em.persist(userSet);
    }
}
