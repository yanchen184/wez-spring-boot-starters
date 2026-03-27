package com.company.common.hub.entity;

import com.company.common.hub.TestJpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HubUserSet Entity 單元測試。
 */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("HubUserSet Entity 測試")
class HubUserSetEntityTest {

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("應建立 HubUserSet 並關聯 HubSet 和 HubUser")
    void shouldCreateHubUserSet_whenAllRelationsProvided() {
        HubSet hubSet = createHubSet("API-1", "/api/users/**");
        HubUser hubUser = createHubUser("system-a");
        em.persist(hubSet);
        em.persist(hubUser);

        HubUserSet userSet = new HubUserSet();
        userSet.setHubSet(hubSet);
        userSet.setHubUser(hubUser);
        userSet.setVerifyDts(LocalDate.of(2026, 1, 1));
        userSet.setVerifyDte(LocalDate.of(2026, 12, 31));
        userSet.setUserVerify(true);
        userSet.setJwtTokenVerify(true);
        userSet.setEnabled(true);

        em.persist(userSet);
        em.flush();

        assertThat(userSet.getId()).isNotNull();
        assertThat(userSet.getHubSet().getId()).isEqualTo(hubSet.getId());
        assertThat(userSet.getHubUser().getId()).isEqualTo(hubUser.getId());
        assertThat(userSet.getVerifyDts()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(userSet.getVerifyDte()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("userVerify 和 jwtTokenVerify 預設值應為 true")
    void shouldDefaultVerifyFieldsToTrue_whenNotSet() {
        HubUserSet userSet = new HubUserSet();

        assertThat(userSet.getUserVerify()).isTrue();
        assertThat(userSet.getJwtTokenVerify()).isTrue();
    }

    private HubSet createHubSet(String name, String uri) {
        HubSet hubSet = new HubSet();
        hubSet.setName(name);
        hubSet.setUri(uri);
        hubSet.setEnabled(true);
        return hubSet;
    }

    private HubUser createHubUser(String username) {
        HubUser hubUser = new HubUser();
        hubUser.setUsername(username);
        hubUser.setPassword("$2a$10$encoded");
        hubUser.setEnabled(true);
        return hubUser;
    }
}
