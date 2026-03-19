package com.company.common.notification.service;

import com.company.common.notification.channel.NotificationChannel;
import com.company.common.notification.dto.NotificationMessage;
import com.company.common.notification.entity.NotificationLog;
import com.company.common.notification.entity.NotificationStatus;
import com.company.common.notification.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core notification service.
 * <p>
 * Supports:
 * <ul>
 *   <li>Immediate sending (async)</li>
 *   <li>Scheduled sending (persisted, picked up by scheduler)</li>
 *   <li>Multi-channel delivery (EMAIL, WEBSOCKET, custom)</li>
 *   <li>Thymeleaf template rendering</li>
 *   <li>Delivery status tracking</li>
 * </ul>
 */
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository logRepository;
    private final Map<String, NotificationChannel> channelMap;
    private final TemplateEngine templateEngine;
    private final Set<String> defaultChannels;

    public NotificationService(NotificationLogRepository logRepository,
                               List<NotificationChannel> channels,
                               TemplateEngine templateEngine,
                               Set<String> defaultChannels) {
        this.logRepository = logRepository;
        this.channelMap = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getChannelName, Function.identity()));
        this.templateEngine = templateEngine;
        this.defaultChannels = defaultChannels;
        log.info("NotificationService initialized with {} channels: {}, defaults: {}",
                channelMap.size(), channelMap.keySet(), defaultChannels);
    }

    /**
     * Send notification immediately (async).
     */
    @Async("notificationExecutor")
    public void send(NotificationMessage message) {
        String renderedContent = renderContent(message);
        Set<String> channels = resolveChannels(message);

        for (Long recipientId : message.getTo()) {
            for (String channelName : channels) {
                NotificationLog entry = createLogEntry(recipientId, channelName, message, renderedContent);
                entry.setStatus(NotificationStatus.SENDING);
                entry = logRepository.save(entry);
                deliverImmediate(entry);
            }
        }
    }

    private void deliverImmediate(NotificationLog entry) {
        NotificationChannel channel = channelMap.get(entry.getChannel());
        if (channel == null) {
            log.error("Unknown notification channel: {}", entry.getChannel());
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorMessage("Unknown channel: " + entry.getChannel());
            logRepository.save(entry);
            return;
        }
        try {
            channel.send(entry);
            entry.setStatus(NotificationStatus.SENT);
            entry.setSentAt(Instant.now());
            logRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to send notification via {} to user {}: {}",
                    entry.getChannel(), entry.getRecipientId(), e.getMessage());
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorMessage(truncate(e.getMessage(), 1000));
            entry.setRetryCount(entry.getRetryCount() + 1);
            logRepository.save(entry);
        }
    }

    /**
     * Schedule notification for future delivery.
     * The notification will be picked up by {@link NotificationScheduler}.
     */
    @Transactional
    public void schedule(NotificationMessage message) {
        if (message.getSendAt() == null) {
            throw new IllegalArgumentException("sendAt is required for scheduled notifications");
        }
        String renderedContent = renderContent(message);
        Set<String> channels = resolveChannels(message);

        for (Long recipientId : message.getTo()) {
            for (String channelName : channels) {
                NotificationLog entry = createLogEntry(recipientId, channelName, message, renderedContent);
                entry.setSendAt(message.getSendAt());
                logRepository.save(entry);
            }
        }
        log.info("Scheduled {} notifications for {} at {}",
                message.getTo().size() * channels.size(), channels, message.getSendAt());
    }

    /**
     * Deliver a single notification log entry.
     */
    @Transactional
    public void deliverSingle(NotificationLog entry) {
        NotificationChannel channel = channelMap.get(entry.getChannel());
        if (channel == null) {
            log.error("Unknown notification channel: {}", entry.getChannel());
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorMessage("Unknown channel: " + entry.getChannel());
            logRepository.save(entry);
            return;
        }

        entry.setStatus(NotificationStatus.SENDING);
        logRepository.save(entry);

        try {
            channel.send(entry);
            entry.setStatus(NotificationStatus.SENT);
            entry.setSentAt(Instant.now());
            entry.setErrorMessage(null);
            logRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to send notification via {} to user {}: {}",
                    entry.getChannel(), entry.getRecipientId(), e.getMessage());
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorMessage(truncate(e.getMessage(), 1000));
            entry.setRetryCount(entry.getRetryCount() + 1);
            logRepository.save(entry);
        }
    }

    /**
     * Get notifications for a recipient.
     */
    public List<NotificationLog> getNotifications(Long recipientId) {
        return logRepository.findByRecipientIdOrderByCreatedDateDesc(recipientId);
    }

    private String renderContent(NotificationMessage message) {
        if (message.getTemplate() != null) {
            Context ctx = new Context();
            ctx.setVariables(message.getData());
            ctx.setVariable("subject", message.getSubject());
            return templateEngine.process("notification/" + message.getTemplate(), ctx);
        }
        return message.getContent();
    }

    private Set<String> resolveChannels(NotificationMessage message) {
        if (message.getChannels() != null && !message.getChannels().isEmpty()) {
            return message.getChannels();
        }
        return defaultChannels;
    }

    private NotificationLog createLogEntry(Long recipientId, String channelName,
                                           NotificationMessage message, String content) {
        NotificationLog entry = new NotificationLog();
        entry.setRecipientId(recipientId);
        entry.setChannel(channelName);
        entry.setSubject(message.getSubject());
        entry.setContent(content);
        entry.setCategory(message.getCategory());
        entry.setStatus(NotificationStatus.PENDING);
        return entry;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
