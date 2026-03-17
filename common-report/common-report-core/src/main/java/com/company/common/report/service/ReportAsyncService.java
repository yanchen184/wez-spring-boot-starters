package com.company.common.report.service;

import com.company.common.report.enums.ReportStatus;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

/**
 * 非同步報表產製服務
 */
public class ReportAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ReportAsyncService.class);

    private final ReportService reportService;
    private final ReportLogService logService;

    public ReportAsyncService(ReportService reportService, ReportLogService logService) {
        this.reportService = reportService;
        this.logService = logService;
    }

    /**
     * 非同步產製報表
     *
     * @param context 產製上下文
     * @param uuid    報表記錄 UUID
     */
    @Async("reportTaskExecutor")
    public void generateAsync(ReportContext context, String uuid) {
        log.info("--> generateAsync | uuid={}, engine={}, format={}",
                uuid, context.getEngineType(), context.getOutputFormat());
        try {
            logService.updateStatus(uuid, ReportStatus.PROCESSING, null);
            ReportResult result = reportService.generate(context);
            logService.saveResult(uuid, result.getContent(), result.getContentType());
            logService.updateStatus(uuid, ReportStatus.COMPLETED, null);
            log.info("<-- generateAsync | uuid={}, COMPLETED", uuid);
        } catch (Exception e) {
            log.error("<-- generateAsync | uuid={}, FAILED: {}", uuid, e.getMessage(), e);
            logService.updateStatus(uuid, ReportStatus.FAILED, e.getMessage());
        }
    }
}
