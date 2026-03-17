package com.company.common.report.autoconfigure;

import com.company.common.report.service.ReportAsyncService;
import com.company.common.report.service.ReportLogService;
import com.company.common.report.service.ReportService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 報表非同步執行配置
 */
@Configuration
@ConditionalOnProperty(prefix = "common.report.async", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableAsync
public class ReportAsyncConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReportAsyncService reportAsyncService(ReportService reportService,
                                                 ReportLogService logService) {
        return new ReportAsyncService(reportService, logService);
    }

    @Bean("reportTaskExecutor")
    public TaskExecutor reportTaskExecutor(ReportProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getAsync().getCorePoolSize());
        executor.setMaxPoolSize(properties.getAsync().getMaxPoolSize());
        executor.setQueueCapacity(properties.getAsync().getQueueCapacity());
        executor.setThreadNamePrefix("report-async-");
        return executor;
    }
}
