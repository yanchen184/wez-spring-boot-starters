package com.company.common.notification.channel;

import com.company.common.notification.entity.NotificationLog;

/**
 * SPI for notification delivery channels.
 * <p>
 * Implementations are auto-discovered and registered by the auto-configuration.
 * To add a custom channel, implement this interface and register it as a Spring Bean.
 *
 * <pre>
 * {@literal @}Component
 * public class SmsChannel implements NotificationChannel {
 *     public String getChannelName() { return "SMS"; }
 *     public void send(NotificationLog log) { ... }
 * }
 * </pre>
 */
public interface NotificationChannel {

    /**
     * @return unique channel name (e.g. "EMAIL", "WEBSOCKET", "SMS")
     */
    String getChannelName();

    /**
     * Send the notification.
     *
     * @param log the notification log entry (contains recipient, subject, content)
     * @throws Exception if delivery fails (will be caught and marked as FAILED)
     */
    void send(NotificationLog log) throws Exception;
}
