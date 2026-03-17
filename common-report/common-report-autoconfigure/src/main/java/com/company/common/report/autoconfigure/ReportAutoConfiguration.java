package com.company.common.report.autoconfigure;

import com.company.common.report.controller.ReportController;
import com.company.common.report.repository.ReportLogBlobRepository;
import com.company.common.report.repository.ReportLogRepository;
import com.company.common.report.service.ReportAsyncService;
import com.company.common.report.service.ReportLogService;
import com.company.common.report.service.ReportService;
import com.company.common.report.spi.ReportEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

/**
 * 報表模組自動配置
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "common.report", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ReportProperties.class)
@EntityScan(basePackages = "com.company.common.report.entity")
@EnableJpaRepositories(basePackages = "com.company.common.report.repository")
public class ReportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReportService reportService(List<ReportEngine> engines) {
        return new ReportService(engines);
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
