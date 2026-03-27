package com.company.common.hub.repository;

import com.company.common.hub.TestJpaConfig;
import com.company.common.hub.entity.HubLog;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HubLogRepository 單元測試。
 */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("HubLogRepository 測試")
class HubLogRepositoryTest {

    @Autowired
    private HubLogRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("應刪除指定時間之前的日誌")
    void shouldDeleteOldLogs_whenCreatedAtBefore() {
        HubLog oldLog = createHubLog("GET", "/api/old");
        oldLog.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        em.persist(oldLog);

        HubLog recentLog = createHubLog("GET", "/api/recent");
        recentLog.setCreatedAt(LocalDateTime.of(2026, 3, 1, 0, 0));
        em.persist(recentLog);
        em.flush();

        repository.deleteByCreatedAtBefore(LocalDateTime.of(2026, 1, 1, 0, 0));
        em.flush();
        em.clear();

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getRequestUri()).isEqualTo("/api/recent");
    }

    @Test
    @DisplayName("無符合條件的日誌時不應刪除任何資料")
    void shouldDeleteNothing_whenNoOldLogs() {
        HubLog recentLog = createHubLog("GET", "/api/recent");
        recentLog.setCreatedAt(LocalDateTime.of(2026, 6, 1, 0, 0));
        em.persist(recentLog);
        em.flush();

        repository.deleteByCreatedAtBefore(LocalDateTime.of(2026, 1, 1, 0, 0));
        em.flush();
        em.clear();

        assertThat(repository.findAll()).hasSize(1);
    }

    private HubLog createHubLog(String method, String uri) {
        HubLog log = new HubLog();
        log.setRequestMethod(method);
        log.setRequestUri(uri);
        log.setIp("127.0.0.1");
        log.setSuccess(true);
        log.setResponseCode("200001");
        log.setElapsedMs(100L);
        return log;
    }
}
