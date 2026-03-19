package com.company.common.notification.entity;

/**
 * Notification delivery status.
 *
 * <pre>
 * PENDING → SENDING → SENT
 *                   ↘ FAILED → (retry) → SENDING → ...
 * </pre>
 */
public enum NotificationStatus {
    /** Queued, waiting to be sent (or scheduled for future delivery). */
    PENDING,
    /** Currently being processed by a channel. */
    SENDING,
    /** Successfully delivered. */
    SENT,
    /** Delivery failed (may be retried). */
    FAILED
}
