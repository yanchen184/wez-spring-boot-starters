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
 * HubUser Entity 單元測試。
 */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("HubUser Entity 測試")
class HubUserEntityTest {

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("應建立 HubUser 並自動產生 ID")
    void shouldCreateHubUser_whenRequiredFieldsProvided() {
        HubUser hubUser = new HubUser();
        hubUser.setUsername("system-a");
        hubUser.setPassword("$2a$10$encodedPassword");
        hubUser.setEnabled(true);

        em.persist(hubUser);
        em.flush();

        assertThat(hubUser.getId()).isNotNull();
        assertThat(hubUser.getUsername()).isEqualTo("system-a");
        assertThat(hubUser.getPassword()).isEqualTo("$2a$10$encodedPassword");
    }

    @Test
    @DisplayName("orgId 應允許 null")
    void shouldAllowNullOrgId_whenNotSet() {
        HubUser hubUser = new HubUser();
        hubUser.setUsername("system-b");
        hubUser.setPassword("$2a$10$encodedPassword");

        em.persist(hubUser);
        em.flush();

        assertThat(hubUser.getOrgId()).isNull();
    }

    @Test
    @DisplayName("應儲存 verifyIp 多行白名單")
    void shouldPersistVerifyIp_whenProvided() {
        HubUser hubUser = new HubUser();
        hubUser.setUsername("system-c");
        hubUser.setPassword("$2a$10$encodedPassword");
        hubUser.setVerifyIp("192.168.1.0/24\n10.0.0.1");

        em.persist(hubUser);
        em.flush();

        assertThat(hubUser.getVerifyIp()).isEqualTo("192.168.1.0/24\n10.0.0.1");
    }

    @Test
    @DisplayName("enabled 預設值應為 true")
    void shouldDefaultEnabledToTrue_whenNotSet() {
        HubUser hubUser = new HubUser();

        assertThat(hubUser.getEnabled()).isTrue();
    }
}
