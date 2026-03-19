package com.company.common.report.autoconfigure;

import com.company.common.report.controller.ReportController;
import com.company.common.report.repository.ReportLogBlobRepository;
import com.company.common.report.repository.ReportLogRepository;
import com.company.common.report.service.ReportAsyncService;
import com.company.common.report.service.ReportLogService;
import com.company.common.report.service.ReportService;
import com.company.common.report.service.ReportThrottleService;
import com.company.common.report.spi.ReportEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * 報表模組自動配置。
 *
 * 注意：@EnableJpaRepositories 是覆蓋式的。
 * 如果使用方自己也有 JPA Repository，需要在 @SpringBootApplication 上加：
 * {@code @EnableJpaRepositories(basePackages = "your.app.repository")}
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "common.report", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ReportProperties.class)
@EntityScan(basePackages = "com.company.common.report.entity")
@EnableJpaRepositories(basePackages = "com.company.common.report.repository")
public class ReportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "common.report.throttle", name = "enabled", matchIfMissing = true)
    public ReportThrottleService reportThrottleService(RedisTemplate<String, Object> redisTemplate,
                                                       ReportProperties properties) {
        ReportProperties.Throttle t = properties.getThrottle();
        return new ReportThrottleService(redisTemplate, t.isEnabled(), t.isGlobalEnabled(),
                t.getGlobalMaxConcurrent(), t.getDefaultMaxConcurrent(), t.getLimits());
    }

    @Bean
    @ConditionalOnMissingBean
    public ReportService reportService(List<ReportEngine> engines,
                                       @Autowired(required = false) ReportThrottleService throttleService) {
        return new ReportService(engines, throttleService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReportLogService reportLogService(ReportLogRepository logRepo,
                                             ReportLogBlobRepository blobRepo) {
        return new ReportLogService(logRepo, blobRepo);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReportController reportController(ReportService reportService,
                                             ReportLogService logService,
                                             @Autowired(required = false)
                                             ReportAsyncService asyncService) {
        return new ReportController(reportService, logService, asyncService);
    }
}
