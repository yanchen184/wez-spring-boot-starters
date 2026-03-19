package com.company.common.notification.repository;

import com.company.common.notification.entity.NotificationLog;
import com.company.common.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /** Find notifications ready to send (PENDING + sendAt <= now or sendAt is null). */
    @Query("SELECT n FROM NotificationLog n WHERE n.status = 'PENDING' "
            + "AND (n.sendAt IS NULL OR n.sendAt <= :now)")
    List<NotificationLog> findReadyToSend(Instant now);

    /** Find failed notifications eligible for retry. */
    @Query("SELECT n FROM NotificationLog n WHERE n.status = 'FAILED' "
            + "AND n.retryCount < :maxRetry")
    List<NotificationLog> findRetryable(int maxRetry);

    /** Find notifications by recipient. */
    List<NotificationLog> findByRecipientIdAndStatusOrderByCreatedDateDesc(
            Long recipientId, NotificationStatus status);

    /** Find all notifications for a recipient. */
    List<NotificationLog> findByRecipientIdOrderByCreatedDateDesc(Long recipientId);

    /** Batch delete old completed/failed notifications. */
    @Modifying
    @Query("DELETE FROM NotificationLog n WHERE n.status IN ('SENT', 'FAILED') "
            + "AND n.createdDate < :before")
    int deleteOlderThan(Instant before);
}
