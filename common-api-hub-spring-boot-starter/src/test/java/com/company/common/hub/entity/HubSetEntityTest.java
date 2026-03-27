package com.company.common.hub.entity;

import com.company.common.hub.TestJpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HubSet Entity 單元測試。
 */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("HubSet Entity 測試")
class HubSetEntityTest {

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("應建立 HubSet 並自動產生 ID")
    void shouldCreateHubSet_whenRequiredFieldsProvided() {
        HubSet hubSet = new HubSet();
        hubSet.setName("使用者查詢 API");
        hubSet.setUri("/api/users/**");
        hubSet.setJwtTokenAging(3600);
        hubSet.setEnabled(true);

        em.persist(hubSet);
        em.flush();

        assertThat(hubSet.getId()).isNotNull();
        assertThat(hubSet.getName()).isEqualTo("使用者查詢 API");
        assertThat(hubSet.getUri()).isEqualTo("/api/users/**");
        assertThat(hubSet.getJwtTokenAging()).isEqualTo(3600);
        assertThat(hubSet.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("jwtTokenAging 預設值應為 3600")
    void shouldDefaultJwtTokenAgingTo3600_whenNotSet() {
        HubSet hubSet = new HubSet();
        hubSet.setName("test");
        hubSet.setUri("/api/test");

        assertThat(hubSet.getJwtTokenAging()).isEqualTo(3600);
    }

    @Test
    @DisplayName("應儲存 description 欄位")
    void shouldPersistDescription_whenProvided() {
        HubSet hubSet = new HubSet();
        hubSet.setName("test");
        hubSet.setUri("/api/test");
        hubSet.setDescription("這是一個測試 API");

        em.persist(hubSet);
        em.flush();

        assertThat(hubSet.getDescription()).isEqualTo("這是一個測試 API");
    }

    @Test
    @DisplayName("enabled 預設值應為 true")
    void shouldDefaultEnabledToTrue_whenNotSet() {
        HubSet hubSet = new HubSet();

        assertThat(hubSet.getEnabled()).isTrue();
    }
}
