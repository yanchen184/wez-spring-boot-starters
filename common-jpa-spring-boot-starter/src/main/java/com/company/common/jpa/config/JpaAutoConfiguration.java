package com.company.common.jpa.config;

import com.company.common.jpa.auditor.DefaultAuditorAware;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 自動配置
 *
 * <p>自動啟用 Spring Data JPA 審計功能，並提供預設的 {@link AuditorAware} 實作。
 *
 * <p>如果消費端已經定義了自己的 {@code AuditorAware} Bean，
 * 預設的 {@link DefaultAuditorAware} 不會被註冊。
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
public class JpaAutoConfiguration {

    /**
     * 啟用 JPA 審計的內部配置。
     * 獨立成靜態內部類，避免與消費端的 @EnableJpaAuditing 衝突。
     */
    @ConditionalOnMissingBean(name = "jpaAuditingHandler")
    @EnableJpaAuditing
    static class JpaAuditingConfig {
    }

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> auditorAware() {
        return new DefaultAuditorAware();
    }
}
