package com.company.common.hub;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 測試用 Spring Boot 應用程式配置。
 *
 * <p>僅用於 Phase 1~4 的 DataJpaTest / Repository 測試。
 * 整合測試使用 {@link IntegrationTestConfig} 額外註冊 Service / Controller。
 */
@SpringBootApplication(scanBasePackages = "com.company.common.hub.repository")
@EntityScan(basePackages = "com.company.common.hub.entity")
@EnableJpaRepositories(basePackages = "com.company.common.hub.repository")
@EnableJpaAuditing
public class TestJpaConfig {
}
