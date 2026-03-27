package com.company.common.hub.repository;

import com.company.common.hub.TestJpaConfig;
import com.company.common.hub.entity.HubSet;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HubSetRepository 單元測試。
 */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("HubSetRepository 測試")
class HubSetRepositoryTest {

    @Autowired
    private HubSetRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("應依 URI 查到對應的 HubSet")
    void shouldFindByUri_whenUriExists() {
        em.persist(createHubSet("API-1", "/api/users/**", true));
        em.persist(createHubSet("API-2", "/api/docs/**", true));
        em.flush();

        Optional<HubSet> found = repository.findByUri("/api/users/**");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("API-1");
    }

    @Test
    @DisplayName("URI 不存在時應回傳空")
    void shouldReturnEmpty_whenUriNotExists() {
        em.persist(createHubSet("API-1", "/api/users/**", true));
        em.flush();

        Optional<HubSet> found = repository.findByUri("/api/nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應只查到啟用的 HubSet")
    void shouldFindOnlyEnabled_whenMixed() {
        em.persist(createHubSet("active", "/api/a", true));
        em.persist(createHubSet("disabled", "/api/b", false));
        em.flush();

        List<HubSet> result = repository.findByEnabledTrue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("active");
    }

    @Test
    @DisplayName("全部停用時應回傳空列表")
    void shouldReturnEmptyList_whenAllDisabled() {
        em.persist(createHubSet("disabled-1", "/api/a", false));
        em.persist(createHubSet("disabled-2", "/api/b", false));
        em.flush();

        List<HubSet> result = repository.findByEnabledTrue();

        assertThat(result).isEmpty();
    }

    private HubSet createHubSet(String name, String uri, boolean enabled) {
        HubSet set = new HubSet();
        set.setName(name);
        set.setUri(uri);
        set.setEnabled(enabled);
        return set;
    }
}
