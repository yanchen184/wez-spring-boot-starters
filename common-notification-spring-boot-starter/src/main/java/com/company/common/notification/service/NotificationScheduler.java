package com.company.common.notification.service;

import com.company.common.notification.entity.NotificationLog;
import com.company.common.notification.entity.NotificationStatus;
import com.company.common.notification.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled tasks for notification processing:
 * <ul>
 *   <li>Process pending/scheduled notifications</li>
 *   <li>Retry failed notifications</li>
 *   <li>Clean up old records</li>
 * </ul>
 */
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationLogRepository logRepository;
    private final NotificationService notificationService;
    private final int maxRetry;
    private final int retentionDays;

    public NotificationScheduler(NotificationLogRepository logRepository,
                                 NotificationService notificationService,
                                 int maxRetry, int retentionDays) {
        this.logRepository = logRepository;
        this.notificationService = notificationService;
        this.maxRetry = maxRetry;
        this.retentionDays = retentionDays;
        log.info("NotificationScheduler initialized: maxRetry={}, retentionDays={}",
                maxRetry, retentionDays);
    }

    /**
     * Process scheduled notifications that are ready to send.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedDelayString = "${common.notification.scheduler.poll-interval-ms:30000}")
    @Transactional
    public void processScheduled() {
        List<NotificationLog> ready = logRepository.findReadyToSend(Instant.now());
        if (ready.isEmpty()) {
            return;
        }
        log.info("Processing {} scheduled notifications", ready.size());
        for (NotificationLog entry : ready) {
            notificationService.deliverSingle(entry);
        }
    }

    /**
     * Retry failed notifications.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${common.notification.scheduler.retry-interval-ms:300000}")
    @Transactional
    public void retryFailed() {
        List<NotificationLog> retryable = logRepository.findRetryable(maxRetry);
        if (retryable.isEmpty()) {
            return;
        }
        log.info("Retrying {} failed notifications (max retry: {})", retryable.size(), maxRetry);
        for (NotificationLog entry : retryable) {
            entry.setStatus(NotificationStatus.PENDING);
            logRepository.save(entry);
            notificationService.deliverSingle(entry);
        }
    }

    /**
     * Clean up old completed/failed notification records.
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "${common.notification.scheduler.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanup() {
        if (retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = logRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} old notification records (older than {} days)",
                    deleted, retentionDays);
        }
    }
}
