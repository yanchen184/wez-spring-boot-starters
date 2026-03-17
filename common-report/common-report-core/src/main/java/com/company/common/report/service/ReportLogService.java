package com.company.common.report.service;

import com.company.common.report.entity.ReportLog;
import com.company.common.report.entity.ReportLogBlob;
import com.company.common.report.enums.ReportStatus;
import com.company.common.report.repository.ReportLogBlobRepository;
import com.company.common.report.repository.ReportLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 報表記錄服務
 */
public class ReportLogService {

    private static final Logger log = LoggerFactory.getLogger(ReportLogService.class);

    private final ReportLogRepository logRepo;
    private final ReportLogBlobRepository blobRepo;

    public ReportLogService(ReportLogRepository logRepo, ReportLogBlobRepository blobRepo) {
        this.logRepo = logRepo;
        this.blobRepo = blobRepo;
    }

    /**
     * 建立 PENDING 狀態的報表記錄
     *
     * @param reportName 報表名稱
     * @param fileName   檔案名稱
     * @return uuid（供後續查詢 / 下載用）
     */
    @Transactional
    public String createLog(String reportName, String fileName) {
        ReportLog reportLog = new ReportLog();
        reportLog.setUuid(UUID.randomUUID().toString());
        reportLog.setReportName(reportName);
        reportLog.setFileName(fileName);
        reportLog.setStatus(ReportStatus.PENDING);
        reportLog.setStartTime(LocalDateTime.now());
        logRepo.save(reportLog);
        log.info("--> createLog | reportName={}, uuid={}", reportName, reportLog.getUuid());
        return reportLog.getUuid();
    }

    /**
     * 更新產製狀態
     */
    @Transactional
    public void updateStatus(String uuid, ReportStatus status, String errorMessage) {
        ReportLog reportLog = logRepo.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("ReportLog not found: " + uuid));
        validateStatusTransition(reportLog.getStatus(), status);
        reportLog.setStatus(status);
        reportLog.setErrorMessage(errorMessage);
        if (status == ReportStatus.COMPLETED || status == ReportStatus.FAILED) {
            reportLog.setEndTime(LocalDateTime.now());
        }
        logRepo.save(reportLog);
        log.info("--> updateStatus | uuid={}, status={}", uuid, status);
    }

    /**
     * 儲存產製結果（檔案內容）
     */
    @Transactional
    public void saveResult(String uuid, byte[] content, String contentType) {
        ReportLog reportLog = logRepo.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("ReportLog not found: " + uuid));
        reportLog.setContentType(contentType);

        ReportLogBlob blob = new ReportLogBlob();
        blob.setReportLog(reportLog);
        blob.setFileBlob(content);
        blobRepo.save(blob);

        log.info("--> saveResult | uuid={}, size={} bytes", uuid, content.length);
    }

    /**
     * 根據 uuid 查詢報表記錄
     */
    @Transactional(readOnly = true)
    public Optional<ReportLog> findByUuid(String uuid) {
        return logRepo.findByUuid(uuid);
    }

    /**
     * 取得檔案 BLOB
     */
    @Transactional(readOnly = true)
    public Optional<byte[]> getBlob(String uuid) {
        return logRepo.findByUuid(uuid)
                .map(ReportLog::getId)
                .flatMap(blobRepo::findById)
                .map(ReportLogBlob::getFileBlob);
    }

    private void validateStatusTransition(ReportStatus from, ReportStatus to) {
        if (from == ReportStatus.COMPLETED || from == ReportStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot transition from " + from + " to " + to + ": terminal state");
        }
        if (from == ReportStatus.PENDING && to != ReportStatus.PROCESSING && to != ReportStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot transition from PENDING to " + to + ": must go through PROCESSING");
        }
    }

    /**
     * 清除超過指定天數的已完成記錄
     */
    @Transactional
    public void cleanupOlderThan(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        logRepo.deleteByCreatedDateBeforeAndStatus(cutoff, ReportStatus.COMPLETED);
        log.info("--> cleanupOlderThan | days={}, cutoff={}", days, cutoff);
    }
}
