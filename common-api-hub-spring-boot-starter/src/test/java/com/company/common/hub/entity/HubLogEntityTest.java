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
 * HubLog Entity 單元測試。
 */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("HubLog Entity 測試")
class HubLogEntityTest {

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("應建立 HubLog 並自動填入 createdAt")
    void shouldAutoSetCreatedAt_whenPersisted() {
        HubLog hubLog = new HubLog();
        hubLog.setRequestMethod("GET");
        hubLog.setRequestUri("/api/users");
        hubLog.setIp("192.168.1.10");
        hubLog.setSuccess(true);
        hubLog.setResponseCode("200001");
        hubLog.setElapsedMs(150L);

        em.persist(hubLog);
        em.flush();

        assertThat(hubLog.getId()).isNotNull();
        assertThat(hubLog.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("hubUserSet 應允許 null（認證失敗時）")
    void shouldAllowNullHubUserSet_whenAuthFailed() {
        HubLog hubLog = new HubLog();
        hubLog.setRequestMethod("POST");
        hubLog.setRequestUri("/api/hub/token");
        hubLog.setIp("192.168.1.10");
        hubLog.setSuccess(false);
        hubLog.setResponseCode("401002");
        hubLog.setErrorLog("帳號不存在");

        em.persist(hubLog);
        em.flush();

        assertThat(hubLog.getId()).isNotNull();
        assertThat(hubLog.getHubUserSet()).isNull();
    }

    @Test
    @DisplayName("應儲存 TEXT 類型欄位（requestParams、responseResult、errorLog）")
    void shouldPersistTextFields_whenProvided() {
        HubLog hubLog = new HubLog();
        hubLog.setRequestMethod("POST");
        hubLog.setRequestUri("/api/users");
        hubLog.setRequestParams("{\"username\":\"admin\",\"password\":\"***\"}");
        hubLog.setIp("10.0.0.1");
        hubLog.setSuccess(true);
        hubLog.setResponseCode("200001");
        hubLog.setResponseResult("{\"code\":\"200001\",\"data\":{}}");
        hubLog.setElapsedMs(200L);

        em.persist(hubLog);
        em.flush();

        assertThat(hubLog.getRequestParams()).contains("username");
        assertThat(hubLog.getResponseResult()).contains("200001");
    }
}
