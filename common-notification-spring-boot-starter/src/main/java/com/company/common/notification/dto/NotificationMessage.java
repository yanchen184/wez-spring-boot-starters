package com.company.common.notification.dto;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Notification message DTO.
 * <p>
 * Use the builder to construct:
 * <pre>
 * NotificationMessage.builder()
 *     .to(List.of(userId1, userId2))
 *     .subject("審核通過")
 *     .template("approval-passed")
 *     .data(Map.of("name", "王小明"))
 *     .channels(Set.of("EMAIL", "WEBSOCKET"))
 *     .build();
 * </pre>
 */
public class NotificationMessage {

    private List<Long> to;
    private String subject;
    private String content;
    private String template;
    private Map<String, Object> data;
    private Set<String> channels;
    private String category;
    private Instant sendAt;

    private NotificationMessage() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Long> getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    /** Direct HTML content. Mutually exclusive with {@link #getTemplate()}. */
    public String getContent() {
        return content;
    }

    /** Thymeleaf template name (under templates/notification/). */
    public String getTemplate() {
        return template;
    }

    /** Template variables. */
    public Map<String, Object> getData() {
        return data;
    }

    /** Channel names to use. Null = use user preferences. */
    public Set<String> getChannels() {
        return channels;
    }

    /** Notification category (e.g. SYSTEM, APPROVAL, ALERT). */
    public String getCategory() {
        return category;
    }

    /** Scheduled send time. Null = send immediately. */
    public Instant getSendAt() {
        return sendAt;
    }

    public boolean isScheduled() {
        return sendAt != null && sendAt.isAfter(Instant.now());
    }

    public static class Builder {
        private final NotificationMessage msg = new NotificationMessage();

        public Builder to(List<Long> userIds) {
            msg.to = userIds;
            return this;
        }

        public Builder to(Long userId) {
            msg.to = List.of(userId);
            return this;
        }

        public Builder subject(String subject) {
            msg.subject = subject;
            return this;
        }

        public Builder content(String htmlContent) {
            msg.content = htmlContent;
            return this;
        }

        public Builder template(String templateName) {
            msg.template = templateName;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            msg.data = data;
            return this;
        }

        public Builder data(String key, Object value) {
            if (msg.data == null) {
                msg.data = new HashMap<>();
            }
            msg.data.put(key, value);
            return this;
        }

        public Builder channels(Set<String> channels) {
            msg.channels = channels;
            return this;
        }

        public Builder channels(String... channels) {
            msg.channels = Set.of(channels);
            return this;
        }

        public Builder category(String category) {
            msg.category = category;
            return this;
        }

        public Builder sendAt(Instant sendAt) {
            msg.sendAt = sendAt;
            return this;
        }

        public NotificationMessage build() {
            if (msg.to == null || msg.to.isEmpty()) {
                throw new IllegalArgumentException("Notification must have at least one recipient");
            }
            if (msg.subject == null || msg.subject.isBlank()) {
                throw new IllegalArgumentException("Notification must have a subject");
            }
            if (msg.content == null && msg.template == null) {
                throw new IllegalArgumentException("Notification must have content or template");
            }
            if (msg.data == null) {
                msg.data = Map.of();
            }
            return msg;
        }
    }
}
